package EShop.lab3

import EShop.lab2.Checkout
import EShop.lab3.Payment.PaymentFinished
import akka.actor.{Actor, ActorRef, Props}

object Payment {
  sealed trait Command
  case object Pay extends Command

  sealed trait Event
  case object PaymentFinished extends Event

  def props(orderManagerRef: ActorRef, checkoutRef: ActorRef) = Props(new Payment(orderManagerRef, checkoutRef))
}

class Payment(orderManagerRef: ActorRef, checkoutRef: ActorRef) extends Actor {
  override def receive: Receive = awaitingPayment

  def awaitingPayment: Receive = {
    case OrderManager.DoPayment =>
      orderManagerRef ! PaymentFinished
      checkoutRef ! Checkout.ReceivePayment

      context become closed
  }

  def closed: Receive = {
    case _ =>
  }
}
