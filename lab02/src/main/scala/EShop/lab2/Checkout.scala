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

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  def receive: Receive = LoggingReceive {
    case StartCheckout =>
      println("Checkout started")

      context become selectingDelivery(null)

      sender ! CheckoutStarted
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case e: SelectDeliveryMethod =>
      println("Selected delivery: " + e.method)

      context become selectingPaymentMethod(null)

      sender ! SelectedDelivery
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case e: SelectPayment =>
      println("Selected payment: " + e.payment)

      context become processingPayment(null)

      sender ! SelectedPayment
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case ReceivePayment =>
      println("Payment received")

      context become closed

      sender ! ReceivedPayment
  }

  def cancelled: Receive = LoggingReceive {
    case _ =>
  }

  def closed: Receive = LoggingReceive {
    case _ =>
  }

}
