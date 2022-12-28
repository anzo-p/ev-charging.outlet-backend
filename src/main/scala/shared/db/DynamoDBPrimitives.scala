package shared.db

import zio.ZIO
import zio.dynamodb.DynamoDBQuery.{get, put}
import zio.dynamodb.{DynamoDBExecutor, DynamoDBQuery, FilterExpression, PrimaryKey}
import zio.schema.Schema

import java.util.UUID

trait DynamoDBPrimitives[T] {

  def schema: Schema[T]

  def tableResource: String
  def primaryKey: String

  private def fetch(q: DynamoDBQuery[Either[String, T]]): ZIO[DynamoDBExecutor, Throwable, T] =
    (for {
      data <- q.execute
    } yield data)
      .flatMap {
        case Left(err) =>
          ZIO.fail(new Throwable(err))
        case Right(a) =>
          ZIO.succeed(a)
      }

  def primK(id: UUID): PrimaryKey =
    PrimaryKey(primaryKey -> id.toString)

  def getByPK(id: UUID): ZIO[DynamoDBExecutor, Throwable, T] =
    fetch {
      get[T](tableResource, primK(id))(schema)
    }

  def getByPK(id: UUID, p: FilterExpression): ZIO[DynamoDBExecutor, Throwable, T] =
    fetch {
      get[T](tableResource, primK(id))(schema).filter(p)
    }

  def putByPK(id: T): ZIO[DynamoDBExecutor, Throwable, Unit] =
    for {
      _ <- put(tableResource, id)(schema).execute
    } yield ()
}
