package EShop.lab3

import EShop.lab2.CartActor
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CartTest
  extends TestKit(ActorSystem("CartTest"))
    with FlatSpecLike
    with ImplicitSender
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  val testItems: Seq[String] = Seq("item1", "item2", "item3", "item4")

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  //use GetItems command which was added to make test easier
  it should "add item properly" in {
    val testActor = TestActorRef(CartActor.props)

    testActor ! CartActor.AddItem(testItems(0))
    testActor ! CartActor.GetItems

    expectMsg(testItems.slice(0, 1))

    testActor ! CartActor.AddItem(testItems(1))
    testActor ! CartActor.AddItem(testItems(2))
    testActor ! CartActor.AddItem(testItems(3))
    testActor ! CartActor.GetItems

    expectMsg(testItems)
  }

  it should "be empty after adding and removing the same item" in {
    val testActor = TestActorRef(CartActor.props)

    testActor ! CartActor.AddItem(testItems(0))
    testActor ! CartActor.RemoveItem(testItems(0))
    testActor ! CartActor.GetItems

    expectMsg(Seq.empty)
  }

  it should "start checkout" in {
    val testActor = TestActorRef(CartActor.props)

    testActor ! CartActor.AddItem(testItems(0))
    testActor ! CartActor.StartCheckout

    expectMsg(_: CartActor.CheckoutStarted)
  }
}
