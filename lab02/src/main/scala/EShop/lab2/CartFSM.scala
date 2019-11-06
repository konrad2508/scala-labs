package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CancelCheckout, CloseCheckout, RemoveItem, StartCheckout}
import EShop.lab2.CartFSM.Status
import akka.actor.{LoggingFSM, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object CartFSM {

  object Status extends Enumeration {
    type Status = Value
    val Empty, NonEmpty, InCheckout = Value
  }

  def props() = Props(new CartFSM())
}

class CartFSM extends LoggingFSM[Status.Value, Cart] {
  import EShop.lab2.CartFSM.Status._

  // useful for debugging, see: https://doc.akka.io/docs/akka/current/fsm.html#rolling-event-log
  override def logDepth = 12

  val cartTimerDuration: FiniteDuration = 1 seconds

  startWith(Empty, Cart.empty)

  when(Empty) {
    case Event(e: AddItem, c: Cart) =>
      goto(NonEmpty).using(c.addItem(e.item))
  }

  when(NonEmpty, stateTimeout = cartTimerDuration) {
    case Event(e: AddItem, c: Cart) =>
      stay().using(c.addItem(e.item))
    case Event(e: RemoveItem, c: Cart) if c.contains(e.item) && c.size == 1 =>
      goto(Empty).using(c.removeItem(e.item))
    case Event(e: RemoveItem, c: Cart) if c.contains(e.item) =>
      stay().using(c.removeItem(e.item))
    case Event(StartCheckout, c: Cart) =>
      goto(InCheckout).using(c)
    case Event(StateTimeout, _) =>
      goto(Empty).using(Cart.empty)
  }

  when(InCheckout) {
    case Event(CancelCheckout, c: Cart) =>
      goto(NonEmpty).using(c)
    case Event(CloseCheckout, c: Cart) =>
      goto(Empty).using(Cart.empty)
  }
}
