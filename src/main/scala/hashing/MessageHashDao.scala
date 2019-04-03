package hashing

import scala.concurrent.{ExecutionContext, Future}
import io.getquill._

trait MessageHashDao {
  def insert(msgHash: MessageHash)(implicit ec: ExecutionContext): Future[Long]
  def findByHash(hash: String)(implicit ec: ExecutionContext): Future[Option[MessageHash]]
}

object MessageHashDao {
  def apply(ctx: PostgresAsyncContext[SnakeCase.type]): MessageHashDao = new MessageHashDaoImpl(ctx)
}

class MessageHashDaoImpl(ctx: PostgresAsyncContext[SnakeCase.type]) extends MessageHashDao {
  import ctx._

  override def insert(msgHash: MessageHash)(implicit ec: ExecutionContext): Future[Long] = {
    ctx.run(query[MessageHash].insert(lift(msgHash)))
  }
  override def findByHash(hash: String)(implicit ec: ExecutionContext): Future[Option[MessageHash]] = {
    ctx.run(query[MessageHash].filter(_.hash == lift(hash))).map(_.headOption)
  }
}
