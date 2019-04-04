import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import hashing.MessageHashService
import io.getquill.{PostgresAsyncContext, SnakeCase}
import org.flywaydb.core.Flyway

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.util.{Failure, Success}

object StartServer extends App {

  implicit val system = ActorSystem("http-server")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val postgresCtx = initDb()

  val messageHashService = new MessageHashService(postgresCtx)
  val routes = messageHashService.routes

  val serverBinding = Http().bindAndHandle(routes, "0.0.0.0", ConfigFactory.load().getInt("port"))
  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)


  private def initDb(): PostgresAsyncContext[SnakeCase.type] = {
    val postgresConfig = ConfigFactory.load().getConfig("postgres")
    val jdbcUrl = postgresConfig.getString("jdbc-url")
    val username = postgresConfig.getString("username")
    val password = postgresConfig.getString("password")

    val flyway: Flyway = Flyway.configure().dataSource(jdbcUrl, username, password).load()
    flyway.migrate()

    val postgresAsyncContextUrl = s"${jdbcUrl}"
    new PostgresAsyncContext(SnakeCase, ConfigFactory.parseMap(Map("url" -> postgresAsyncContextUrl).asJava))
  }
}
