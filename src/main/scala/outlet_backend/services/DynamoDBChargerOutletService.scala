package outlet_backend.services

import outlet_backend.ChargerOutletService
import outlet_backend.types.chargerOutlet.ChargerOutlet
import shared.db.DynamoDBPrimitives
import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState
import shared.types.outletStateMachine.OutletStateMachine._
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb._
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class DynamoDBChargerOutletService(executor: DynamoDBExecutor)
    extends ChargerOutletService
    with DynamoDBPrimitives[ChargerOutlet]
    with DateTimeSchemaImplicits {

  val tableResource = "ev-charging_charger-outlet_table"
  val primaryKey    = "outletId"

  override def schema: Schema[ChargerOutlet] = DeriveSchema.gen[ChargerOutlet]

  private def getByOutletIdAndRfidTag(outletId: UUID, rfidTag: Option[String]): ZIO[DynamoDBExecutor, Throwable, ChargerOutlet] =
    for {
      result <- getByPK(outletId).filterOrFail(r => r.rfidTag == rfidTag.getOrElse(r.rfidTag))(
                 new Error(s"no data found for outletId: $outletId and rfidTag: $rfidTag"))
    } yield result

  private def checkAndSetState(
      outletId: UUID,
      rfidTag: Option[String],
      nextState: OutletDeviceState,
      message: String
    ): Task[Unit] =
    (for {
      data     <- getByOutletIdAndRfidTag(outletId, rfidTag)
      filtered <- ZIO.succeed(data).filterOrFail(_.mayTransitionTo(nextState))(new Error(message))
      updated  <- ZIO.succeed(filtered.copy(outletState = nextState))
      _        <- putByPK(updated)
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  private def aggregateConsumption(
      data: ChargerOutlet,
      state: OutletDeviceState,
      endTime: Option[java.time.OffsetDateTime],
      powerConsumption: Double
    ): ZIO[Any, Nothing, ChargerOutlet] =
    ZIO.succeed(
      data.copy(
        outletState           = state,
        endTime               = endTime,
        powerConsumption      = data.powerConsumption + powerConsumption,
        totalPowerConsumption = data.totalPowerConsumption + powerConsumption
      ))

  override def getOutlet(outletId: UUID): Task[ChargerOutlet] =
    (for {
      outlet <- getByPK(outletId)
    } yield outlet)
      .provideLayer(ZLayer.succeed(executor))

  override def register(outlet: ChargerOutlet): Task[Unit] =
    put(tableResource, outlet).execute.provideLayer(ZLayer.succeed(executor))

  override def checkTransitionOrElse(outletId: UUID, nextState: OutletDeviceState, message: String): Task[Unit] =
    (for {
      outlet <- getByOutletIdAndRfidTag(outletId, None)
      _      <- ZIO.from(outlet).filterOrFail(_.mayTransitionTo(nextState))(new Error(message))
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def setAvailable(outletId: UUID): Task[Unit] = {
    val targetState = OutletDeviceState.Available
    checkAndSetState(outletId, None, targetState, cannotTransitionTo(targetState))
  }

  override def setCablePlugged(outletId: UUID): Task[Unit] = {
    val targetState = OutletDeviceState.CablePlugged
    checkAndSetState(outletId, None, targetState, cannotTransitionTo(targetState))
  }

  override def setCharging(outletId: UUID, rfidTag: String, sessionId: UUID): Task[Unit] = {
    val targetState = OutletDeviceState.Charging
    (for {
      data <- getByPK(outletId).filterOrFail(_.mayTransitionTo(targetState))(new Error(cannotTransitionTo(targetState)))

      update <- ZIO.succeed(
                 data.copy(
                   outletState         = targetState,
                   rfidTag             = rfidTag,
                   sessionId           = Some(sessionId),
                   startTime           = java.time.OffsetDateTime.now(),
                   powerConsumption    = 0,
                   totalChargingEvents = data.totalChargingEvents + 1
                 ))

      _ <- putByPK(update)
    } yield ())
      .provideLayer(ZLayer.succeed(executor))
  }

  override def aggregateConsumption(outlet: ChargerOutlet): Task[ChargerOutlet] = {
    val targetState = OutletDeviceState.Charging
    (for {
      data   <- getByPK(outlet.outletId).filterOrFail(_.mayTransitionTo(targetState))(new Error(cannotTransitionTo(targetState)))
      update <- aggregateConsumption(data, targetState, outlet.endTime, outlet.powerConsumption)
      _      <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))
  }

  override def stopCharging(outlet: ChargerOutlet): Task[ChargerOutlet] = {
    val targetState = OutletDeviceState.ChargingFinished
    (for {
      data <- getByOutletIdAndRfidTag(outlet.outletId, Some(outlet.rfidTag))
               .filterOrFail(_.mayTransitionTo(targetState))(new Error(cannotTransitionTo(targetState)))

      update <- aggregateConsumption(data, targetState, outlet.endTime, outlet.powerConsumption)
      _      <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))
  }
}

object DynamoDBChargerOutletService {

  val live: ZLayer[DynamoDBExecutor, Nothing, ChargerOutletService] =
    ZLayer.fromFunction(DynamoDBChargerOutletService.apply _)
}
