package EShop.lab4

import EShop.lab2.{Cart, Checkout}
import akka.actor.{Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.PersistentActor

import scala.concurrent.duration._

object PersistentCartActor {

  def props(persistenceId: String) = Props(new PersistentCartActor(persistenceId))
}

class PersistentCartActor(
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration: FiniteDuration = 5.seconds

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  private def scheduleTimer: Cancellable = context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  override def receiveCommand: Receive = empty

  private def updateState(event: Event, timer: Option[Cancellable] = None): Unit = {
    timer.foreach(_.cancel())
    event match {
      case CartExpired | CheckoutClosed | CartEmptied      => context become empty
      case CheckoutCancelled(cart)            => context become nonEmpty(cart, scheduleTimer)
      case ItemAdded(item, cart)              => context become nonEmpty(cart, scheduleTimer)
      case ItemRemoved(item, cart)            => context become nonEmpty(cart, scheduleTimer)
      case CheckoutStarted(checkoutRef, cart) => context become inCheckout(cart)
    }
  }

  def empty: Receive = LoggingReceive {
    case e: AddItem =>
      var cart = Cart.empty
      cart = cart addItem e.item

      persist(ItemAdded(e.item, cart)) {
        ev => updateState(ev)
      }

    case GetItems => sender ! Seq.empty
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case e: AddItem =>
      val newCart = cart addItem e.item

      persist(ItemAdded(e.item, newCart)) { ev => updateState(ev, Some(timer)) }

    case e: RemoveItem if (cart contains e.item) && cart.size == 1 =>
      persist(CartEmptied) { ev => updateState(ev, Some(timer)) }

    case e: RemoveItem if cart contains e.item =>
      val newCart = cart removeItem e.item

      persist(ItemRemoved(e.item, newCart)) { ev => updateState(ev, Some(timer)) }

    case ExpireCart => persist(CartEmptied) { ev => updateState(ev, Some(timer)) }

    case StartCheckout =>
      val checkoutRef = context.system.actorOf(PersistentCheckout.props(self, "checkout"))

      persist(CheckoutStarted(checkoutRef, cart)) { ev =>
        sender ! ev
        checkoutRef ! Checkout.StartCheckout
        updateState(ev, Some(timer))
      }

    case GetItems => sender ! cart.items
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case CancelCheckout => persist(CheckoutCancelled(cart)) { ev => updateState(ev) }
    case CloseCheckout => persist(CheckoutClosed) { ev => updateState(ev) }
  }

  override def receiveRecover: Receive = {
    case e: Event => updateState(e)
  }
}
