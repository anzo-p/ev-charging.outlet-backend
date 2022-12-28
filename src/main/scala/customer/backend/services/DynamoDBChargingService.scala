package customer.backend.services

import customer.backend.ChargingService
import customer.backend.types.chargingSession.ChargingSession
import shared.db.DynamoDBPrimitives
import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb.PartitionKeyExpression.PartitionKey
import zio.dynamodb.ProjectionExpression.$
import zio.dynamodb._
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class DynamoDBChargingService(executor: DynamoDBExecutor)
    extends ChargingService
    with DynamoDBPrimitives[ChargingSession]
    with DateTimeSchemaImplicits {

  val tableResource = "ev-outlet-app.charging-session.table"
  val primaryKey    = "sessionId"

  override def schema: Schema[ChargingSession] = DeriveSchema.gen[ChargingSession]

  private def getActiveSessions(customerId: UUID): ZIO[DynamoDBExecutor, Throwable, Long] =
    for {
      query <- queryAll[ChargingSession](tableResource)
                .indexName("customerId-on-charging-session-index")
                .whereKey(PartitionKey("customerId") === customerId.toString)
                .filter($("sessionState") in Set(OutletDeviceState.ChargingRequested.entryName, OutletDeviceState.Charging.entryName)) // rather collect them all and filter in scala
                .execute

      result <- query.runCount
    } yield result

  override def initialize(session: ChargingSession): ZIO[Any, Throwable, Unit] =
    (for {
      already <- getActiveSessions(session.customerId)
      _       <- ZIO.fromEither(Either.cond(already == 0, (), new Error("customer already has active session")))
      _       <- put(tableResource, session).execute
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def setStopRequested(sessionId: UUID): ZIO[Any, Throwable, Unit] =
    (for {
      data <- getByPK(sessionId, $("state") === OutletDeviceState.Charging.entryName)
      _    <- putByPK(data.copy(sessionState = OutletDeviceState.StoppingRequested))
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def getHistory(customerId: UUID): Task[List[ChargingSession]] =
    (for {
      query <- queryAll[ChargingSession](tableResource)
                .whereKey(PartitionKey("customerId") === customerId.toString)
                .execute

      result <- query.runCollect
    } yield result
      .sortBy(_.startTime.getOrElse(java.time.OffsetDateTime.MIN))
      .toList
      .reverse)
      .provideLayer(ZLayer.succeed(executor))
}

object DynamoDBChargingService {

  val live: ZLayer[DynamoDBExecutor, Nothing, ChargingService] =
    ZLayer.fromFunction(DynamoDBChargingService.apply _)
}

/*
  dynamodb
  - schema - since we generalized concepts we had top provide schema all the way down, now it seems schemas must be defined at service, not in the models
  - unsafe operations - many unexpected things can go wrongh., this time our field names collided with aws reseverd words, bring out a list dn filter in scala
 */