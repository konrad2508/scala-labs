package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import akka.actor.{Actor, ActorRef, Props}

object OrderManager {
  sealed trait Command
  case class AddItem(item: Any) extends Command
  case class RemoveItem(item: Any) extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case class SelectPaymentMethod(method: String) extends Command
  case object StartCheckout extends Command
  case object DoPayment extends Command

  def props = Props(new OrderManager())
}

class OrderManager extends Actor {
  override def receive: Receive = {
    val cartRef = context.system.actorOf(CartActor.props)
    ordering(cartRef)
  }

  def ordering(cartRef: ActorRef): Receive = {
    case e: AddItem => cartRef ! CartActor.AddItem(e.item)
    case e: RemoveItem => cartRef ! CartActor.RemoveItem(e.item)
    case StartCheckout => cartRef ! CartActor.StartCheckout
  }

  def checkingOut(checkoutRef: ActorRef): Receive = {
    case e: SelectDeliveryMethod => checkoutRef ! Checkout.SelectDeliveryMethod(e.method)
    case e: SelectPaymentMethod => checkoutRef ! Checkout.SelectPayment(e.method)
  }

  def payment(paymentRef: ActorRef): Receive = {
    case DoPayment => paymentRef ! Payment.Pay
  }
}