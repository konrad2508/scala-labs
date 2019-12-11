package EShop.lab5

import EShop.lab5.ProductCatalog.{GetItems, Items}
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.util.Timeout
import akka.pattern.ask
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ProductCatalogController(system: ActorSystem) extends HttpApp with JsonSupport {
  private implicit val timeout: Timeout     = Timeout(5.seconds)
  override protected def routes: Route = {
    path("products") {
      get {
        parameters(Symbol("brand"), Symbol("keywords")) { (brand, keywords) =>
          val keywordsList = keywords.split(" ").toList
          val query = GetItems(brand, keywordsList)

          val catalog = system.actorSelection("akka.tcp://ProductCatalog@127.0.0.1:2553/user/productcatalog")

          val items = (for {
            actor <- catalog.resolveOne()
            response <- actor ? query
          } yield response).mapTo[Items]

          complete{ items }
        }
      }
    }
  }
}

object ProductCatalogController extends App {
  val config  = ConfigFactory.load()

  val serverActorSystem = ActorSystem(
    "ProductCatalogServer",
    config.getConfig("productcatalogserver").withFallback(config)
  )

  val catalogCatalogSystem = ActorSystem(
    "ProductCatalog",
    config.getConfig("productcatalog").withFallback(config)
  )

  catalogCatalogSystem.actorOf(
    ProductCatalog.props(new SearchService()),
    "productcatalog"
  )

  new ProductCatalogController(serverActorSystem).startServer("localhost", 5000, serverActorSystem)
}
