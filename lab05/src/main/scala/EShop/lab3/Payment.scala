package EShop.lab3

import EShop.lab2.Checkout
import EShop.lab3.Payment.{DoPayment, PaymentConfirmed}
import akka.actor.{Actor, ActorRef, Props}

object Payment {

  sealed trait Command
  case object DoPayment extends Command

  sealed trait Event
  case object PaymentConfirmed extends Event

  sealed trait Data
  case object Empty extends Data

  sealed trait State
  case object WaitingForPayment extends State

  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new Payment(method, orderManager, checkout))

}

class Payment(method: String, orderManagerRef: ActorRef, checkoutRef: ActorRef) extends Actor {
  override def receive: Receive = {
    case DoPayment =>
      orderManagerRef ! PaymentConfirmed
      checkoutRef ! Checkout.ReceivePayment

      context become closed
  }

  def closed: Receive = {
    case _ =>
  }
}
