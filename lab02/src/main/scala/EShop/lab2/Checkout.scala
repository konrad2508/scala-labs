package EShop.lab2

import EShop.lab2.Checkout._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ReceivePayment                      extends Command

  sealed trait Event
  case object CheckoutStarted                  extends Event
  case object CheckOutClosed                   extends Event
  case class PaymentStarted(payment: ActorRef) extends Event
  case object SelectedDelivery                 extends Event
  case object SelectedPayment                  extends Event
  case object ReceivedPayment                  extends Event

  def props(cart: ActorRef) = Props(new Checkout())
}

class Checkout extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration: FiniteDuration = 5 seconds
  val paymentTimerDuration: FiniteDuration  = 5 seconds

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  private def scheduleTimer(dur: FiniteDuration, msg: Command): Cancellable =
    context.system.scheduler.scheduleOnce(dur, self, msg)

  def receive: Receive = LoggingReceive {
    case StartCheckout =>
      val timeout = scheduleTimer(checkoutTimerDuration, ExpireCheckout)

      context become selectingDelivery(timeout)
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case _: SelectDeliveryMethod =>
      timer.cancel()

      val timeout = scheduleTimer(checkoutTimerDuration, ExpireCheckout)

      context become selectingPaymentMethod(timeout)

    case CancelCheckout =>
      timer.cancel()

      context become cancelled

    case ExpireCheckout =>
      context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case _: SelectPayment =>
      timer.cancel()

      val timeout = scheduleTimer(paymentTimerDuration, ExpirePayment)

      context become processingPayment(timeout)

    case CancelCheckout =>
      timer.cancel()

      context become cancelled

    case ExpireCheckout =>
      context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case ReceivePayment =>
      timer.cancel()

      context become closed

    case CancelCheckout =>
      timer.cancel()

      context become cancelled

    case ExpirePayment =>
      context become cancelled
  }

  def cancelled: Receive = LoggingReceive {
    case _ =>
  }

  def closed: Receive = LoggingReceive {
    case _ =>
  }
}
