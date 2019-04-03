package hashing

import java.security.MessageDigest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, entity, path, pathEnd, pathPrefix, _}
import akka.http.scaladsl.server.PathMatchers.{PathEnd, Segment}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import io.getquill.{PostgresAsyncContext, SnakeCase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageHashService(postgresCtx: PostgresAsyncContext[SnakeCase.type]) {

  private val msgHashDao = MessageHashDao(postgresCtx)

  val routes: Route =
    pathPrefix("messages") {
      path(Segment ~ PathEnd) { hash =>
        get {
          val msgHashOptFut = msgHashDao.findByHash(hash)
          onSuccess(msgHashOptFut) {
            case Some(msgHash) => complete(StatusCodes.OK, msgHash.msg)
            case None => complete(StatusCodes.NotFound)
          }
        }
      } ~
      pathEnd {
        post {
          entity(as[String]) { message =>
            val sha = shaSum(message)
            val insertFut: Future[Long] = msgHashDao.insert(MessageHash(0L, message, sha))
            onSuccess(insertFut) { _ =>
              complete(StatusCodes.OK, sha)
            }
          }
        }
      }
    }

  private def shaSum(input: String): String = {
    MessageDigest.getInstance("SHA-256")
      .digest(input.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
  }
}
