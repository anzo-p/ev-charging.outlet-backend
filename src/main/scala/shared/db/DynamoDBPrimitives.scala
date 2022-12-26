package shared.db

import outlet.backend.types.chargerOutlet.ChargerOutlet
import zio.ZIO
import zio.dynamodb.DynamoDBQuery.{get, put}
import zio.dynamodb.{DynamoDBExecutor, FilterExpression, PrimaryKey}

import java.util.UUID

trait DynamoDBPrimitives {
  def tableResource: String
  def primaryKey: String

  private def pk(outletId: UUID): PrimaryKey =
    PrimaryKey(primaryKey -> outletId.toString)

  def getByPK(outletId: UUID, p: FilterExpression): ZIO[DynamoDBExecutor, Throwable, ChargerOutlet] =
    (for {
      data <- get[ChargerOutlet](tableResource, pk(outletId)).filter(p).execute
    } yield data)
      .flatMap {
        case Left(err) =>
          ZIO.fail(new Throwable(err))
        case Right(a) =>
          ZIO.succeed(a)
      }

  def putByPK(outlet: ChargerOutlet): ZIO[DynamoDBExecutor, Throwable, Unit] =
    for {
      _ <- put(tableResource, outlet).execute
    } yield ()
}
