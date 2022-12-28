package app.backend.services

import app.backend.types.chargingSession.ChargingSession
import ChargingSession.mayTransitionTo
import app.backend.ChargingService
import shared.db.DynamoDBPrimitives
import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb.PartitionKeyExpression.PartitionKey
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

  private def getActiveSessions(customerId: UUID): ZIO[DynamoDBExecutor, Throwable, Int] =
    for {
      query <- queryAll[ChargingSession](tableResource)
                .indexName("customerId-on-charging-session-index")
                .whereKey(PartitionKey("customerId") === customerId.toString)
                .execute

      result <- query
                 .runCollect
                 .map(
                   _.toList
                     .count(_.state.in(Seq(OutletDeviceState.ChargingRequested, OutletDeviceState.Charging))))
    } yield result

  override def initialize(session: ChargingSession): Task[Unit] =
    (for {
      already <- getActiveSessions(session.customerId)
      _       <- ZIO.fromEither(Either.cond(already == 0, (), new Error("customer already has active session")))
      _       <- put(tableResource, session).execute
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def setStopRequested(sessionId: UUID): Task[Unit] =
    (for {
      data <- getByPK(sessionId).filterOrDie(mayTransitionTo(OutletDeviceState.Charging))(new Error("no data found"))
      _    <- putByPK(data.copy(state = OutletDeviceState.StoppingRequested))
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def getSession(sessionId: UUID): Task[ChargingSession] =
    (for {
      session <- getByPK(sessionId)
    } yield session)
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
