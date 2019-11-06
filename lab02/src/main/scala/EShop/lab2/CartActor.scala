package EShop.lab2

import EShop.lab2.CartActor._
import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props, Timers}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {
  sealed trait Command
  case class AddItem(item: Any)    extends Command
  case class RemoveItem(item: Any) extends Command
  case object ExpireCart           extends Command
  case object StartCheckout        extends Command
  case object CancelCheckout       extends Command
  case object CloseCheckout        extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef)   extends Event
  case class CheckoutCancelled(checkoutRef: ActorRef) extends Event
  case class CheckoutClosed(checkoutRef: ActorRef)    extends Event
  case object ItemAdded                               extends Event
  case object ItemRemoved                             extends Event
  case object CartExpired                             extends Event

  def props = Props(new CartActor())
}

class CartActor extends Actor {
  private val log                       = Logging(context.system, this)
  val cartTimerDuration: FiniteDuration = 5 seconds

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  private def scheduleTimer: Cancellable             = context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case e: AddItem =>
      var cart = Cart.empty
      cart = cart addItem e.item

//      sender ! ItemAdded

      context become nonEmpty(cart, scheduleTimer)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case e: AddItem =>
      timer.cancel()

      val newCart = cart addItem e.item

//      sender ! ItemAdded

      context become nonEmpty(newCart, scheduleTimer)

    case e: RemoveItem if (cart contains e.item) && cart.size == 1 =>
      timer.cancel()

      cart removeItem e.item

//      sender ! ItemRemoved

      context become empty

    case e: RemoveItem if cart contains e.item =>
      timer.cancel()

      val newCart = cart removeItem e.item

//      sender ! ItemRemoved

      context become nonEmpty(newCart, scheduleTimer)

    case ExpireCart =>
      timer.cancel()

//      sender ! CartExpired

      context become empty

    case StartCheckout =>
      timer.cancel()
//      sender ! CheckoutStarted

      context become inCheckout(cart)
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case CancelCheckout =>
//      sender ! CheckoutCancelled

      context become nonEmpty(cart, scheduleTimer)

    case CloseCheckout =>
//      sender ! CheckoutClosed

      context become empty
  }
}
