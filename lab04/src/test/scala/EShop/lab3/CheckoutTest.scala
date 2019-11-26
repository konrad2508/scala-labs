package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CheckoutTest
  extends TestKit(ActorSystem("CheckoutTest"))
    with FlatSpecLike
    with ImplicitSender
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  val deliveryMethod = "delivery"
  val paymentMethod = "payment"

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  it should "Send close confirmation to cart" in {
    val testParent = TestProbe()
    val testChild = testParent.childActorOf(Checkout.props(testParent.ref, system.deadLetters))

    testChild ! Checkout.StartCheckout
    testChild ! Checkout.SelectDeliveryMethod(deliveryMethod)
    testChild ! Checkout.SelectPayment(paymentMethod)
    testChild ! Checkout.ReceivePayment

    testParent.expectMsg(CartActor.CloseCheckout)
  }
}
