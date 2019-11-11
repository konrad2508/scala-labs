package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CloseCheckout, RemoveItem, StartCheckout}
import EShop.lab2.Checkout.{CancelCheckout, ReceivePayment, SelectDeliveryMethod, SelectPayment}
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}

object Application extends App {
  def printHelp(state: Int): Unit = {
    state match {
      case 0 =>
        println("1. Add item")
        println("2. Remove item")
        println("3. Start checkout")
      case 1 =>
        println("1. Select delivery")
        println("2. Select payment")
        println("3. Pay")
        println("4. Cancel checkout")
    }
  }

  val system    = ActorSystem("TestoSystem")
  val cart      = system.actorOf(Props[CartActor], name = "cart1")
  var checkout  = ActorRef.noSender
  var checkoutN = 1

  var state = 0

  while (true) {
    printHelp(state)

    val decision = scala.io.StdIn.readInt()

    state match {
      case 0 =>
        decision match {
          case 1 =>
            print("Item name: ")
            val item = scala.io.StdIn.readLine()
            cart ! AddItem(item)
          case 2 =>
            print("Item name: ")
            val item = scala.io.StdIn.readLine()
            cart ! RemoveItem(item)
          case 3 =>
            cart ! StartCheckout

            checkout = system.actorOf(Props[Checkout], name = "checkout" + checkoutN)
            checkoutN += 1
            checkout ! Checkout.StartCheckout

            state = 1
        }
      case 1 =>
        decision match {
          case 1 =>
            print("Delivery method: ")
            val delivery = scala.io.StdIn.readLine()
            checkout ! SelectDeliveryMethod(delivery)
          case 2 =>
            print("Payment method: ")
            val payment = scala.io.StdIn.readLine()
            checkout ! SelectPayment(payment)
          case 3 =>
            checkout ! ReceivePayment
            checkout ! PoisonPill
            cart ! CloseCheckout
            state = 0
          case 4 =>
            checkout ! CancelCheckout
            checkout ! PoisonPill
            cart ! CartActor.CancelCheckout
            state = 0
        }
    }
  }
}
