package EShop.lab2

import EShop.lab2.CartActor._
import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
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
  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable = ???

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case e: AddItem =>
      var cart = Cart.empty
      cart = cart addItem e.item
      println("Adding " + e.item)

      context become nonEmpty(cart, null)

      sender ! ItemAdded
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case e: AddItem =>
      val newCart = cart addItem e.item
      println("Adding " + e.item)

      context become nonEmpty(newCart, null)

      sender ! ItemAdded

    case e: RemoveItem if cart.size == 1 =>
      cart removeItem e.item
      println("Removing " + e.item + " (last item)")

      context become empty

      sender ! ItemRemoved

    case e: RemoveItem =>
      val newCart = cart removeItem e.item
      println("Removing " + e.item)

      context become nonEmpty(newCart, null)

      sender ! ItemRemoved

    case ExpireCart =>
      println("Cart expired")

      context become empty

      sender ! CartExpired

    case StartCheckout =>
      println("Checkout started")

      context become inCheckout(cart)

      sender ! CheckoutStarted
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case CancelCheckout =>
      println("Checkout cancelled")

      context become nonEmpty(cart, null)

      sender ! CheckoutCancelled

    case CloseCheckout =>
      println("Checkout closed")

      context become empty

      sender ! CheckoutClosed
  }
}

object MainApp extends App {
  val system = ActorSystem("TestoSystem")
  val cart   = system.actorOf(Props[CartActor], name = "cart1")
}
