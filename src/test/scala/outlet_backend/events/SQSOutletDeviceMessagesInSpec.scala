package outlet_backend.events

import outlet_backend.types.chargerOutlet.ChargerOutlet
import outlet_backend.types.outletDeviceMessage.Fixtures.fixtureBasicDeviceMessage
import outlet_backend.utils.mocks.{MockChargerOutletService, MockChargingEventProducer}
import shared.types.chargingEvent.{ChargingEvent, EventSession}
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio.ZIO
import zio.mock.Expectation
import zio.test.{assertTrue, Assertion, ZIOSpecDefault}

import java.util.UUID

object SQSOutletDeviceMessagesInSpec extends ZIOSpecDefault {

  override def spec =
    suite("SQSOutletDeviceMessagesInSpec")(
      test("handle case Available") {
        val sqsConsumer = ZIO.serviceWithZIO[SQSOutletDeviceMessagesIn](_.consume(fixtureBasicDeviceMessage))

        val mockOutletService = MockChargerOutletService.SetAvailable(
          Assertion.equalTo(fixtureBasicDeviceMessage.outletId),
          Expectation.unit
        )

        for {
          _ <- sqsConsumer
                .provide(
                  SQSOutletDeviceMessagesIn.live,
                  mockOutletService,
                  MockChargingEventProducer.empty
                )
        } yield assertTrue(true)
      },
      test("handle case CablePlugged") {
        val testDeviceMessage = fixtureBasicDeviceMessage.copy(
          outletStateChange = OutletDeviceState.CablePlugged
        )

        val sqsConsumer = ZIO.serviceWithZIO[SQSOutletDeviceMessagesIn](_.consume(testDeviceMessage))

        val mockOutletService = MockChargerOutletService.SetCablePlugged(
          Assertion.equalTo(testDeviceMessage.outletId),
          Expectation.unit
        )

        for {
          _ <- sqsConsumer
                .provide(
                  SQSOutletDeviceMessagesIn.live,
                  mockOutletService,
                  MockChargingEventProducer.empty
                )
        } yield assertTrue(true)
      },
      test("DeviceRequestsCharging") {
        val testDeviceMessage = fixtureBasicDeviceMessage.copy(
          outletStateChange = OutletDeviceState.DeviceRequestsCharging
        )

        val testProduceBackendEvent = ChargingEvent(
          initiator   = EventInitiator.OutletBackend,
          outletId    = testDeviceMessage.outletId,
          outletState = testDeviceMessage.outletStateChange,
          recentSession = EventSession(
            sessionId        = None,
            rfidTag          = fixtureBasicDeviceMessage.rfidTag,
            periodStart      = testDeviceMessage.periodStart,
            periodEnd        = Some(testDeviceMessage.periodEnd),
            powerConsumption = 0.0
          )
        )

        val sqsConsumer = ZIO.serviceWithZIO[SQSOutletDeviceMessagesIn](_.consume(testDeviceMessage))

        val mockOutletService = MockChargerOutletService.CheckTransitionOrElse(
          Assertion.equalTo(
            (
              testDeviceMessage.outletId,
              testDeviceMessage.outletStateChange,
              "Device already has active session"
            )),
          Expectation.unit
        )

        val mockToBackend = MockChargingEventProducer.Put(
          Assertion.equalTo(testProduceBackendEvent),
          Expectation.unit
        )

        for {
          _ <- sqsConsumer
                .provide(
                  SQSOutletDeviceMessagesIn.live,
                  mockOutletService,
                  mockToBackend
                )
        } yield assertTrue(true)
      },
      test("Charging") {
        val testDeviceMessage = fixtureBasicDeviceMessage.copy(
          periodStart       = java.time.OffsetDateTime.now().minusMinutes(2L),
          periodEnd         = java.time.OffsetDateTime.now().minusSeconds(10L),
          outletStateChange = OutletDeviceState.Charging,
          powerConsumption  = 0.333
        )

        val testChargerOutlet = new ChargerOutlet(
          outletId              = testDeviceMessage.outletId,
          chargerGroupId        = UUID.randomUUID(),
          outletCode            = "String",
          address               = "String",
          maxPower              = 22.0,
          outletState           = OutletDeviceState.Charging,
          sessionId             = Some(UUID.randomUUID()),
          rfidTag               = testDeviceMessage.rfidTag,
          startTime             = fixtureBasicDeviceMessage.periodStart,
          endTime               = Some(testDeviceMessage.periodEnd),
          powerConsumption      = 2,
          totalChargingEvents   = 12345L,
          totalPowerConsumption = 1000.0
        )

        val testEventToBackend = ChargingEvent(
          initiator   = EventInitiator.OutletBackend,
          outletId    = testDeviceMessage.outletId,
          outletState = testDeviceMessage.outletStateChange,
          recentSession = EventSession(
            sessionId        = testChargerOutlet.sessionId,
            rfidTag          = testDeviceMessage.rfidTag,
            periodStart      = fixtureBasicDeviceMessage.periodStart,
            periodEnd        = Some(testDeviceMessage.periodEnd),
            powerConsumption = testChargerOutlet.powerConsumption
          )
        )

        val sqsConsumer = ZIO.serviceWithZIO[SQSOutletDeviceMessagesIn](_.consume(testDeviceMessage))

        val mockOutletService = {
          MockChargerOutletService.GetOutlet(
            Assertion.equalTo(testDeviceMessage.outletId),
            Expectation.value(testChargerOutlet)
          ) ++
            MockChargerOutletService.AggregateConsumption(
              Assertion.equalTo(testChargerOutlet.setChargingFrom(testDeviceMessage)),
              Expectation.value(testChargerOutlet)
            )
        }

        val mockToBackend = MockChargingEventProducer.Put(
          Assertion.equalTo(testEventToBackend),
          Expectation.unit
        )

        for {
          _ <- sqsConsumer
                .provide(
                  SQSOutletDeviceMessagesIn.live,
                  mockOutletService,
                  mockToBackend
                )
        } yield assertTrue(true)
      },
      test("DeviceRequestsStop") {
        val testDeviceMessage = fixtureBasicDeviceMessage.copy(
          periodStart       = java.time.OffsetDateTime.now().minusMinutes(2L),
          periodEnd         = java.time.OffsetDateTime.now().minusSeconds(10L),
          outletStateChange = OutletDeviceState.DeviceRequestsStop,
          powerConsumption  = 0.333
        )

        val testChargerOutletBefore = new ChargerOutlet(
          outletId              = testDeviceMessage.outletId,
          chargerGroupId        = UUID.randomUUID(),
          outletCode            = "String",
          address               = "String",
          maxPower              = 22.0,
          outletState           = OutletDeviceState.Charging,
          sessionId             = Some(UUID.randomUUID()),
          rfidTag               = testDeviceMessage.rfidTag,
          startTime             = fixtureBasicDeviceMessage.periodStart,
          endTime               = Some(testDeviceMessage.periodEnd.minusMinutes(2L)),
          powerConsumption      = 2,
          totalChargingEvents   = 12345L,
          totalPowerConsumption = 1000.0
        )

        val testChargingOutletAfter = testChargerOutletBefore.copy(
          outletState           = OutletDeviceState.DeviceRequestsStop,
          endTime               = Some(testDeviceMessage.periodEnd),
          powerConsumption      = testChargerOutletBefore.powerConsumption + testDeviceMessage.powerConsumption,
          totalPowerConsumption = testChargerOutletBefore.totalPowerConsumption + testDeviceMessage.powerConsumption
        )

        val testToBackend = ChargingEvent(
          initiator   = EventInitiator.OutletBackend,
          outletId    = testDeviceMessage.outletId,
          outletState = testChargingOutletAfter.outletState,
          recentSession = EventSession(
            sessionId        = testChargerOutletBefore.sessionId,
            rfidTag          = testDeviceMessage.rfidTag,
            periodStart      = testChargerOutletBefore.startTime,
            periodEnd        = testChargingOutletAfter.endTime,
            powerConsumption = testChargingOutletAfter.powerConsumption
          )
        )

        val sqsConsumer = ZIO.serviceWithZIO[SQSOutletDeviceMessagesIn](_.consume(testDeviceMessage))

        val mockOutletService =
          MockChargerOutletService.GetOutlet(
            Assertion.equalTo(testDeviceMessage.outletId),
            Expectation.value(testChargerOutletBefore)
          ) ++
            MockChargerOutletService.StopCharging(
              Assertion.equalTo(testChargerOutletBefore.getUpdatesFrom(testDeviceMessage)),
              Expectation.value(testChargingOutletAfter)
            )

        val mockToBackend = MockChargingEventProducer.Put(
          Assertion.equalTo(testToBackend),
          Expectation.unit
        )

        for {
          _ <- sqsConsumer
                .provide(
                  SQSOutletDeviceMessagesIn.live,
                  mockOutletService,
                  mockToBackend
                )
        } yield assertTrue(true)
      }
    )
}
