package customer.backend.service

import customer.backend.ChargingService
import shared.types.ChargingSession
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb.PartitionKeyExpression.PartitionKey
import zio.dynamodb._

import java.util.UUID

final case class DynamoDBChargingService(executor: DynamoDBExecutor) extends ChargingService {

  override def hasActiveSession(customerId: UUID): IO[Throwable, Boolean] =
    (for {
      query <- queryAll[ChargingSession]("ev-outlet-app.charging-session.table")
                .indexName("customerId-on-charging-session-index")
                .whereKey(PartitionKey("customerId") === customerId.toString)
                //.filter($("customerId").exists)
                .execute
      result <- query.runCollect
    } yield result.headOption)
      .map {
        case None =>
          false
        case Some(_) =>
          true
      }
      .provideLayer(ZLayer.succeed(executor))

  override def add(session: ChargingSession): IO[Throwable, ChargingSession] =
    (for {
      session <- ZIO.succeed(session)
      _       <- put("ev-outlet-app.charging-session.table", session).execute
    } yield session)
      .provideLayer(ZLayer.succeed(executor))

  override def update(sessionId: UUID, a: ChargingSession): IO[Throwable, ChargingSession] = ???
}

object DynamoDBChargingService {

  val tableResource = "ev-outlet-app.charging-session.table"

  val live: ZLayer[DynamoDBExecutor, Nothing, ChargingService] =
    ZLayer.fromFunction(DynamoDBChargingService.apply _)
}
