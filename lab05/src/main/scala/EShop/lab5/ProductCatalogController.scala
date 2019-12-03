package EShop.lab5

import EShop.lab5.ProductCatalog.{GetItems, Items}
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ProductCatalogController(system: ActorSystem) extends HttpApp {
  private implicit val timeout: Timeout     = Timeout(5.seconds)
  override protected def routes: Route = {
    path("products") {
      get {
        parameters(Symbol("brand"), Symbol("keywords")) { (brand, keywords) =>
          val keywordsList = keywords.split(" ").toList
          val query = GetItems(brand, keywordsList)

          val catalog = system.actorSelection("akka.tcp://ProductCatalog@127.0.0.1:2553/user/productcatalog")

          complete {
            (for {
              actor <- catalog.resolveOne()
              response <- actor ? query
            } yield response).mapTo[Items]
          }
        }
      }
    }
  }
}

object Main extends App {
  private val config  = ConfigFactory.load()
  val httpActorSystem = ActorSystem("server", config.getConfig("server").withFallback(config))

  private val productCatalogSystem = ActorSystem(
    "ProductCatalog",
    config.getConfig("productcatalog").withFallback(config)
  )

  productCatalogSystem.actorOf(
    ProductCatalog.props(new SearchService()),
    "productcatalog"
  )

  val server = new ProductCatalogController(httpActorSystem)
  server.startServer("localhost", 8888, httpActorSystem)
}
