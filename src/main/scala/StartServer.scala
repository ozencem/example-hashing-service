import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import hashing.MessageHashService
import io.getquill.{PostgresAsyncContext, SnakeCase}
import org.flywaydb.core.Flyway

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.util.{Failure, Success}

object StartServer extends App {

  val postgresConfig = ConfigFactory.load().getConfig("postgres")
  val flyway: Flyway = Flyway.configure().dataSource(postgresConfig.getString("jdbc-url"),
                                                     postgresConfig.getString("username"),
                                                     postgresConfig.getString("password")).load()
  flyway.migrate()

  implicit val system = ActorSystem("http-server")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val postgresCtx = new PostgresAsyncContext(SnakeCase, "postgres-context")

  val messageHashService = new MessageHashService(postgresCtx)
  val routes = messageHashService.routes

  val serverBinding = Http().bindAndHandle(routes, "localhost", 8080)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
