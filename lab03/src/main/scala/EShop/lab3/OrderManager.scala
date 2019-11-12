package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive

object OrderManager {
  sealed trait State
  case object Uninitialized extends State
  case object Open          extends State
  case object InCheckout    extends State
  case object InPayment     extends State
  case object Finished      extends State

  sealed trait Command
  case class AddItem(id: String)                                               extends Command
  case class RemoveItem(id: String)                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command
  case object Buy                                                              extends Command
  case object Pay                                                              extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK

  sealed trait Data
  case object Empty                                                            extends Data
  case class CartData(cartRef: ActorRef)                                       extends Data
  case class CartDataWithSender(cartRef: ActorRef, sender: ActorRef)           extends Data
  case class InCheckoutData(checkoutRef: ActorRef)                             extends Data
  case class InCheckoutDataWithSender(checkoutRef: ActorRef, sender: ActorRef) extends Data
  case class InPaymentData(paymentRef: ActorRef)                               extends Data
  case class InPaymentDataWithSender(paymentRef: ActorRef, sender: ActorRef)   extends Data
}

class OrderManager extends Actor {

  override def receive: Receive = uninitialized

  def uninitialized: Receive = LoggingReceive {
    val cartRef = context.system.actorOf(CartActor.props(self))
    open(cartRef)
  }

  def open(cartActor: ActorRef): Receive = LoggingReceive {
    case e: AddItem => cartActor ! CartActor.AddItem(e.id)
    case e: RemoveItem => cartActor ! CartActor.RemoveItem(e.id)
    case Buy =>
      cartActor ! CartActor.StartCheckout
      context become inCheckout(cartActor, sender)
  }

  def inCheckout(cartActorRef: ActorRef, senderRef: ActorRef): Receive = LoggingReceive {
    case e: CartActor.CheckoutStarted => context become inCheckout(e.checkoutRef, senderRef)
  }

  def inCheckout(checkoutActorRef: ActorRef, senderRef: ActorRef): Receive = LoggingReceive {
    case e: SelectDeliveryAndPaymentMethod =>
      checkoutActorRef ! Checkout.SelectDeliveryMethod(e.delivery)
      checkoutActorRef ! Checkout.SelectPayment(e.payment)

      context become inPayment(senderRef)
  }

  def inPayment(senderRef: ActorRef): Receive = LoggingReceive {
    case Checkout.PaymentStarted(paymentRef) => context become inPayment(paymentRef, senderRef)

  }

  def inPayment(paymentActorRef: ActorRef, senderRef: ActorRef): Receive = LoggingReceive {
    case Pay                      => paymentActorRef ! Payment.Pay
    case Payment.PaymentConfirmed => context become finished
  }

  def finished: Receive = LoggingReceive {
    case _ => sender ! "order manager finished job"
  }
}
