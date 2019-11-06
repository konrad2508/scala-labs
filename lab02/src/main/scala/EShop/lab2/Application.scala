package EShop.lab2

import akka.actor.{ActorSystem, Props}

object Application extends App {
  val system = ActorSystem("TestoSystem")
  val cart   = system.actorOf(Props[CartActor], name = "cart1")
}
