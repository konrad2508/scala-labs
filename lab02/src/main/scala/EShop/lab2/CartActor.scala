package EShop.lab2

import EShop.lab2.CartActor.{ItemAdded, ItemRemoved}
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
  case class CheckoutStarted(checkoutRef: ActorRef) extends Event
  case class CheckoutCancelled(checkoutRef: ActorRef) extends Event
  case class CheckoutClosed(checkoutRef: ActorRef) extends Event
  case object ItemAdded extends Event
  case object ItemRemoved extends Event

  def props = Props(new CartActor())
}

class CartActor extends Actor {
  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable = ???

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case ItemAdded =>
      val cart = new Cart(null)
      cart.addItem()
      println("Adding sth (empty)")
      context become nonEmpty(cart, null)
    case _ => ???
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case ItemAdded =>
      cart.addItem()
      println("Adding sth (nonEmpty)")
    case ItemRemoved if cart.size != 1 =>
      cart.removeItem()
      println("Removing sth (nonEmpty)")
    case ItemRemoved if cart.size == 1 =>
      cart.removeItem()
      println("Removing sth (nowEmpty)")
      context become empty
    case _ => ???
  }

  def inCheckout(cart: Cart): Receive = ???

}

object MainApp extends App {
  val system = ActorSystem("TestoSystem")
  val cart = system.actorOf(Props[CartActor], name = "cart1")

  cart ! ItemAdded
  cart ! ItemAdded
  cart ! ItemAdded
  cart ! ItemRemoved
  cart ! ItemRemoved
  cart ! ItemRemoved
}
