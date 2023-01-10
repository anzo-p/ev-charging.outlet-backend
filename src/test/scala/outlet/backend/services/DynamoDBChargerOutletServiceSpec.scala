package outlet.backend.services

import outlet.backend.ChargerOutletService
import outlet.backend.system.{LocalAWSConfig, LocalDynamoDB}
import outlet.backend.types.chargerOutlet.Fixtures.fixtureBasicChargerOutlet
import shared.types.chargingEvent.{ChargingEvent, EventSession}
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio.ZIO
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb._
import zio.test._

import java.util.UUID

object DynamoDBChargerOutletServiceSpec extends ZIOSpecDefault {

  override def spec = {
    suite("dynamodb")(
      suite("register and get")(
        test("register and getOutlet") {
          val testOutlet = fixtureBasicChargerOutlet.copy(outletId = UUID.randomUUID())
          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet.copy(outletId = UUID.randomUUID())))
            outlet <- ZIO.serviceWithZIO[ChargerOutletService](_.getOutlet(testOutlet.outletId))
          } yield assertTrue(outlet.get == testOutlet)
        },
        test("checkTransitionOrElse succeeds") {
          val testOutlet = fixtureBasicChargerOutlet.copy(outletId = UUID.randomUUID())
          for {
            _ <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            result <- ZIO.serviceWithZIO[ChargerOutletService](
                       _.checkTransitionOrElse(
                         testOutlet.outletId,
                         OutletDeviceState.CablePlugged,
                         ""
                       ))
          } yield assertTrue(result == ())
        }
      ),
      suite("set states")(
        /*
        // failures must .exit before they are assertable and this seems to shut down dynamopDB test container
        test("checkTransitionOrElse fails") {
          val testOutlet = testChargerOutlet.copy(outletId = UUID.randomUUID())
          val z = (for {
            _ <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            _ <- ZIO.serviceWithZIO[ChargerOutletService](
                  _.checkTransitionOrElse(
                    testOutlet.outletId,
                    OutletDeviceState.Charging,
                    "fails"
                  ))
          } yield ()).flip
          assertZIO(z)(hasMessage(equalTo("fails")))
        },
         */
        test("setAvailable succeeds") {
          val testOutlet = fixtureBasicChargerOutlet.copy(outletId = UUID.randomUUID(), outletState = OutletDeviceState.CablePlugged)
          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.setAvailable(testOutlet.outletId))
            result <- ZIO.serviceWithZIO[ChargerOutletService](_.getOutlet(testOutlet.outletId))
          } yield assertTrue(result.get.outletState == OutletDeviceState.Available)
        },
        test("setCablePlugged succeeds") {
          val testOutlet = fixtureBasicChargerOutlet.copy(outletId = UUID.randomUUID(), outletState = OutletDeviceState.Available)
          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.setCablePlugged(testOutlet.outletId))
            result <- ZIO.serviceWithZIO[ChargerOutletService](_.getOutlet(testOutlet.outletId))
          } yield assertTrue(result.get.outletState == OutletDeviceState.CablePlugged)
        },
        test("setCharging succeeds") {
          val testBegins = java.time.OffsetDateTime.now()
          val testRfid   = UUID.randomUUID().toString

          val testOutlet = fixtureBasicChargerOutlet.copy(
            outletId    = UUID.randomUUID(),
            outletState = OutletDeviceState.CablePlugged
          )

          val expected = testOutlet.copy(
            outletState         = OutletDeviceState.Charging,
            rfidTag             = testRfid,
            totalChargingEvents = testOutlet.totalChargingEvents + 1L
          )
          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.setCharging(testOutlet.outletId, rfidTag = testRfid))
            result <- ZIO.serviceWithZIO[ChargerOutletService](_.getOutlet(testOutlet.outletId))
          } yield assertTrue(
            result.get.startTime.isAfter(testBegins) && result.get.startTime.isBefore(java.time.OffsetDateTime.now()) &&
              result.get.copy(startTime = expected.startTime) == expected
          )
        }
      ),
      suite("aggregate and stop")(
        test("aggregateConsumption succeeds") {
          val testOutlet = fixtureBasicChargerOutlet.copy(
            outletId            = UUID.randomUUID(),
            outletState         = OutletDeviceState.Charging,
            rfidTag             = UUID.randomUUID().toString,
            totalChargingEvents = 1L
          )

          val testChargingEvent = new ChargingEvent(
            initiator   = EventInitiator.AppBackend,
            outletId    = testOutlet.outletId,
            outletState = OutletDeviceState.Charging,
            recentSession = EventSession(
              sessionId        = Some(UUID.randomUUID()),
              rfidTag          = testOutlet.rfidTag,
              periodStart      = java.time.OffsetDateTime.now().minusMinutes(2L),
              periodEnd        = Some(java.time.OffsetDateTime.now()),
              powerConsumption = 1.0
            )
          )

          val expected = testOutlet.copy(
            outletState           = OutletDeviceState.Charging,
            endTime               = testChargingEvent.recentSession.periodEnd,
            powerConsumption      = testOutlet.powerConsumption + testChargingEvent.recentSession.powerConsumption,
            totalPowerConsumption = testOutlet.totalPowerConsumption + testChargingEvent.recentSession.powerConsumption
          )

          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            result <- ZIO.serviceWithZIO[ChargerOutletService](_.aggregateConsumption(testChargingEvent))
          } yield assertTrue(result == expected)
        },
        test("stopCharging succeeds") {
          val testOutlet = fixtureBasicChargerOutlet.copy(
            outletId            = UUID.randomUUID(),
            outletState         = OutletDeviceState.Charging,
            rfidTag             = UUID.randomUUID().toString,
            powerConsumption    = 1.0,
            totalChargingEvents = 1L
          )

          val testChargingEvent = new ChargingEvent(
            initiator   = EventInitiator.AppBackend,
            outletId    = testOutlet.outletId,
            outletState = OutletDeviceState.AppRequestsStop,
            recentSession = EventSession(
              sessionId        = Some(UUID.randomUUID()),
              rfidTag          = testOutlet.rfidTag,
              periodStart      = java.time.OffsetDateTime.now().minusMinutes(2L),
              periodEnd        = Some(java.time.OffsetDateTime.now()),
              powerConsumption = 1.0
            )
          )

          val expected = testOutlet.copy(
            outletState           = OutletDeviceState.ChargingFinished,
            endTime               = testChargingEvent.recentSession.periodEnd,
            powerConsumption      = testOutlet.powerConsumption + testChargingEvent.recentSession.powerConsumption,
            totalPowerConsumption = testOutlet.totalPowerConsumption + testChargingEvent.recentSession.powerConsumption
          )

          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            result <- ZIO.serviceWithZIO[ChargerOutletService](_.stopCharging(testChargingEvent))
          } yield assertTrue(result == expected)
        }
      )
    ) @@ TestAspect.beforeAll {
      {
        deleteTable("ev-charging_charger-outlet_table")
          .execute
          .ignore
      } *> {
        createTable("ev-charging_charger-outlet_table", KeySchema("outletId"), BillingMode.PayPerRequest)(
          AttributeDefinition.attrDefnString("outletId")
        ).execute
      }
    }
  }.provide(
    DynamoDBChargerOutletService.live,
    DynamoDBExecutor.live,
    LocalAWSConfig.awsConfig,
    LocalDynamoDB.layer
  )
}
