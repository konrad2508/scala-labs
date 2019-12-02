package EShop.lab4

import EShop.lab2.CartActor
import EShop.lab3.Payment
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.PersistentActor

import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object PersistentCheckout {

  def props(cartActor: ActorRef, persistenceId: String) =
    Props(new PersistentCheckout(cartActor, persistenceId))
}

class PersistentCheckout(
  cartRef: ActorRef,
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.Checkout._
  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration: FiniteDuration = 5.seconds
  val paymentTimerDuration: FiniteDuration  = 5.seconds

  private def scheduleTimer(dur: FiniteDuration, msg: Command): Cancellable =
    context.system.scheduler.scheduleOnce(dur, self, msg)

  private def updateState(event: Event, maybeTimer: Option[Cancellable] = None): Unit = {
    maybeTimer.foreach(_.cancel())
    event match {
      case CheckoutStarted                => context become selectingDelivery(scheduleTimer(checkoutTimerDuration, ExpireCheckout))
      case SelectedDelivery(method)       => context become selectingPaymentMethod(scheduleTimer(checkoutTimerDuration, ExpireCheckout))
      case CheckoutClosed                 => context become closed
      case CheckoutCancelled              => context become cancelled
      case PaymentStarted(payment)        => context become processingPayment(scheduleTimer(paymentTimerDuration, ExpirePayment))

    }
  }

  def receiveCommand: Receive = LoggingReceive {
    case StartCheckout =>
      persist(CheckoutStarted) { ev => updateState(ev) }
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case e: SelectDeliveryMethod =>
      persist(SelectedDelivery(e.method)) { ev => updateState(ev, Some(timer)) }

    case CancelCheckout | ExpireCheckout =>
      persist(CheckoutCancelled) { ev => updateState(ev) }
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case e: SelectPayment =>
      val paymentRef = context.system.actorOf(Payment.props(e.payment, sender, self))
      persist(PaymentStarted(paymentRef)) { ev =>
        sender ! ev
        updateState(ev, Some(timer))
      }

    case CancelCheckout | ExpireCheckout =>
      persist(CheckoutCancelled) { ev => updateState(ev) }
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case ReceivePayment =>
      persist(CheckoutClosed) { ev =>
        cartRef ! CartActor.CloseCheckout
        updateState(ev, Some(timer))
      }

    case CancelCheckout | ExpirePayment =>
      persist(CheckoutCancelled) { ev => updateState(ev) }
  }

  def cancelled: Receive = LoggingReceive {
    case _ =>
  }

  def closed: Receive = LoggingReceive {
    case _ =>
  }

  override def receiveRecover: Receive = {
    case e: Event => updateState(e)
  }
}
