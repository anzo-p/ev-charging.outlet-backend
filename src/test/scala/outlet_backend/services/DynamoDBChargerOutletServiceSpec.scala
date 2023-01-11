package outlet_backend.services

import outlet_backend.ChargerOutletService
import outlet_backend.types.chargerOutlet.Fixtures.fixtureBasicChargerOutlet
import outlet_backend.types.outletDeviceMessage.OutletDeviceMessage
import outlet_backend.utils.{LocalAWSConfig, LocalDynamoDB}
import shared.types.enums.OutletDeviceState
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
          } yield assertTrue(outlet == testOutlet)
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
          } yield assertTrue(result.outletState == OutletDeviceState.Available)
        },
        test("setCablePlugged succeeds") {
          val testOutlet = fixtureBasicChargerOutlet.copy(outletId = UUID.randomUUID(), outletState = OutletDeviceState.Available)
          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.setCablePlugged(testOutlet.outletId))
            result <- ZIO.serviceWithZIO[ChargerOutletService](_.getOutlet(testOutlet.outletId))
          } yield assertTrue(result.outletState == OutletDeviceState.CablePlugged)
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
            result.startTime.isAfter(testBegins) && result.startTime.isBefore(java.time.OffsetDateTime.now()) &&
              result.copy(startTime = expected.startTime) == expected
          )
        }
      ),
      suite("aggregate and stop")(
        test("aggregateConsumption succeeds") {
          val testOutlet = fixtureBasicChargerOutlet.copy(
            outletId              = UUID.randomUUID(),
            outletState           = OutletDeviceState.Charging,
            rfidTag               = UUID.randomUUID().toString,
            powerConsumption      = 2.0,
            totalChargingEvents   = 1L,
            totalPowerConsumption = 111.0
          )

          val testUpdateFromDevice = OutletDeviceMessage(
            outletId          = testOutlet.outletId,
            rfidTag           = testOutlet.rfidTag,
            periodStart       = java.time.OffsetDateTime.now().minusMinutes(2L),
            periodEnd         = java.time.OffsetDateTime.now().minusSeconds(10L),
            outletStateChange = OutletDeviceState.Charging,
            powerConsumption  = 0.333
          )

          val expected = testOutlet.copy(
            outletState           = OutletDeviceState.Charging,
            endTime               = Some(testUpdateFromDevice.periodEnd),
            powerConsumption      = testOutlet.powerConsumption + testUpdateFromDevice.powerConsumption,
            totalPowerConsumption = testOutlet.totalPowerConsumption + testUpdateFromDevice.powerConsumption
          )

          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            result <- ZIO.serviceWithZIO[ChargerOutletService](_.aggregateConsumption(testOutlet.getUpdatesFrom(testUpdateFromDevice)))
          } yield assertTrue(result == expected)
        },
        test("stopCharging succeeds") {
          val testOutlet = fixtureBasicChargerOutlet.copy(
            outletId              = UUID.randomUUID(),
            outletState           = OutletDeviceState.Charging,
            rfidTag               = UUID.randomUUID().toString,
            powerConsumption      = 2.0,
            totalChargingEvents   = 1L,
            totalPowerConsumption = 111.0
          )

          val testUpdateFromDevice = OutletDeviceMessage(
            outletId          = testOutlet.outletId,
            rfidTag           = testOutlet.rfidTag,
            periodStart       = java.time.OffsetDateTime.now().minusMinutes(2L),
            periodEnd         = java.time.OffsetDateTime.now().minusSeconds(10L),
            outletStateChange = OutletDeviceState.Charging,
            powerConsumption  = 0.333
          )

          val expected = testOutlet.copy(
            outletState           = OutletDeviceState.ChargingFinished,
            endTime               = Some(testUpdateFromDevice.periodEnd),
            powerConsumption      = testOutlet.powerConsumption + testUpdateFromDevice.powerConsumption,
            totalPowerConsumption = testOutlet.totalPowerConsumption + testUpdateFromDevice.powerConsumption
          )

          for {
            _      <- ZIO.serviceWithZIO[ChargerOutletService](_.register(testOutlet))
            result <- ZIO.serviceWithZIO[ChargerOutletService](_.stopCharging(testOutlet.getUpdatesFrom(testUpdateFromDevice)))
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
