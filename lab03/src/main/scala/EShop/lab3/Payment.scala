package EShop.lab3

import akka.actor.{Actor, Props}

object Payment {
  sealed trait Command
  case object Pay extends Command

  sealed trait Event
  case object PaymentFinished extends Event

  def props = Props(new Payment())
}

class Payment extends Actor {
  override def receive: Receive = ???
}
