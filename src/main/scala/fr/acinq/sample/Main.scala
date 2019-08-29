package fr.acinq.sample

import java.sql.DriverManager
import java.time.LocalDateTime
import java.util.logging.LogManager

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import fr.acinq.sample.Utils.{InfoResponse, InfoResponseSerializer, PersonSerializer, PointSerializer}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object Main extends LazyLogging with Directives with Json4sSupport {

  def main(args: Array[String]): Unit = {
    configureLogging()

    val config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem("graal", config)
    implicit val materializer: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val formats = org.json4s.DefaultFormats + InfoResponseSerializer + PointSerializer + PersonSerializer
    implicit val serialization = org.json4s.jackson.Serialization

    Class.forName("org.sqlite.JDBC")
    val database = new Database(DriverManager.getConnection("jdbc:sqlite::memory"))
    database.createDb()

    val route = get {
        path("graal-hp-size") {
          onSuccess(graalHomepageSize) { size =>
            complete(size.toString)
        }
      } ~ path("json") {
          complete(InfoResponse(date = LocalDateTime.now().toString))
        } ~ path("scodec") {
          complete(Utils.showScodecUsage())
        } ~ path("jheaps") {
          complete(Utils.showJHeapUsage())
        } ~ path("commons") {
          complete(s"LightningNerwork == Base32(${Utils.encodeBase32("LightningNerwork")})")
        } ~ path("query") {
          parameter('name) { name =>
            complete(database.byName(name))
          }
        }
    }


    Http()
      .bindAndHandle(route,
                     config.getString("http.service.bind-to"),
                     config.getInt("http.service.port"))
      .andThen {
        case Success(binding) => logger.info(s"Listening at ${binding.localAddress}")
      }

  }

  private def graalHomepageSize(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer): Future[Int] = {
    Http().singleRequest(HttpRequest(uri = "https://www.graalvm.org")).flatMap { resp =>
      resp.status match {
        case StatusCodes.OK =>
          resp.entity.dataBytes.runFold(0) { (cnt, chunk) =>
            cnt + chunk.size
          }
        case other =>
          resp.discardEntityBytes()
          throw new IllegalStateException(s"Unexpected status code $other")
      }
    }
  }

  private def configureLogging(): Unit = {
    val is = getClass.getResourceAsStream("/app.logging.properties")
    try {
      LogManager.getLogManager.reset()
      LogManager.getLogManager.readConfiguration(is)
    }
    finally is.close()
  }

}
