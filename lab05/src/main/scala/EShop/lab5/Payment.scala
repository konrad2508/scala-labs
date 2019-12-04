package EShop.lab5

import EShop.lab2.Checkout
import EShop.lab3.Payment.{DoPayment, PaymentConfirmed}
import EShop.lab5.Payment.{PaymentRejected, PaymentRestarted}
import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Terminated}

import scala.concurrent.duration._

object Payment {

  case object PaymentRejected
  case object PaymentRestarted

  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new Payment(method, orderManager, checkout))

}

class Payment(
               method: String,
               orderManagerRef: ActorRef,
               checkoutRef: ActorRef
) extends Actor
  with ActorLogging {

  var paymentService: ActorRef = _

  override def receive: Receive = {
    case DoPayment =>
      paymentService = context.actorOf(PaymentService.props(method, self))
      context.watch(paymentService)

    case PaymentSucceeded =>
      orderManagerRef ! PaymentConfirmed
      checkoutRef ! Checkout.ReceivePayment

    case e: Terminated if e.actor == paymentService => notifyAboutRejection()
  }

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1.seconds) {
      case _: PaymentServerError =>
        notifyAboutRestart()
        Restart

      case _: PaymentClientError =>
        notifyAboutRejection()
        Stop

      case _: Exception =>
        notifyAboutRejection()
        Escalate
    }

  //please use this one to notify when supervised actor was stoped
  private def notifyAboutRejection(): Unit = {
    orderManagerRef ! PaymentRejected
    checkoutRef ! PaymentRejected
  }

  //please use this one to notify when supervised actor was restarted
  private def notifyAboutRestart(): Unit = {
    orderManagerRef ! PaymentRestarted
    checkoutRef ! PaymentRestarted
  }
}
