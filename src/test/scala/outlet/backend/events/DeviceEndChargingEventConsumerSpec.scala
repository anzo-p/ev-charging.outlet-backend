package outlet.backend.events

import outlet.backend.mocks.{MockChargerOutletService, MockChargingEventProducer, MockDeadLettersProducer, MockOutletDeviceMessageProducer}
import outlet.backend.types.chargerOutlet.ChargerOutlet
import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import shared.types.chargingEvent.{ChargingEvent, EventSession}
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio._
import zio.mock.Expectation
import zio.test._

import java.util.UUID

object DeviceEndChargingEventConsumerSpec extends ZIOSpecDefault {

  val fixtureBasicChargingEvent: ChargingEvent =
    ChargingEvent(
      initiator   = EventInitiator.OutletBackend,
      outletId    = UUID.fromString("e55b9d6b-5951-4ae9-9260-c505cf762bd0"),
      outletState = OutletDeviceState.Available,
      recentSession = EventSession(
        sessionId        = None,
        rfidTag          = "",
        periodStart      = java.time.OffsetDateTime.now().minusMinutes(5L),
        periodEnd        = None,
        powerConsumption = 0.0
      )
    )

  def spec =
    suite("KinesisChargingEventsIn")(
      test("handle case AppRequestsCharging") {
        val testSession = fixtureBasicChargingEvent
          .recentSession
          .copy(
            sessionId        = Some(UUID.randomUUID()),
            rfidTag          = UUID.randomUUID().toString,
            periodStart      = java.time.OffsetDateTime.now().minusSeconds(20L),
            periodEnd        = Some(java.time.OffsetDateTime.now()),
            powerConsumption = 0.0
          )

        val testProduceDeviceEvent = OutletDeviceMessage(
          outletId         = fixtureBasicChargingEvent.outletId,
          rfidTag          = testSession.rfidTag,
          periodStart      = testSession.periodStart,
          periodEnd        = testSession.periodEnd.getOrElse(java.time.OffsetDateTime.now()),
          outletStatus     = OutletDeviceState.Charging,
          powerConsumption = testSession.powerConsumption
        )

        val kinesisConsumer = ZIO.serviceWithZIO[KinesisChargingEventsIn](
          _.consume(
            fixtureBasicChargingEvent.copy(
              outletState   = OutletDeviceState.AppRequestsCharging,
              recentSession = testSession
            ))
        )

        val mockOutletService =
          MockChargerOutletService.CheckTransitionOrElse(
            Assertion.equalTo(
              (fixtureBasicChargingEvent.outletId, OutletDeviceState.AppRequestsCharging, "Device already has active session")),
            Expectation.unit
          ) ++
            MockChargerOutletService.SetCharging(
              Assertion.equalTo((fixtureBasicChargingEvent.outletId, testSession.rfidTag)),
              Expectation.unit
            )

        val mockToDevice = MockOutletDeviceMessageProducer
          .Produce(
            Assertion.equalTo(testProduceDeviceEvent),
            Expectation.unit
          )

        for {
          _ <- kinesisConsumer
                .provide(
                  KinesisChargingEventsIn.live,
                  MockChargingEventProducer.empty,
                  mockToDevice,
                  mockOutletService,
                  MockDeadLettersProducer.empty
                )
        } yield assertTrue(true)
      },
      test("handle case Charging") {
        val testSession = fixtureBasicChargingEvent
          .recentSession
          .copy(
            sessionId        = Some(UUID.randomUUID()),
            rfidTag          = UUID.randomUUID().toString,
            periodStart      = java.time.OffsetDateTime.now().minusMinutes(2L),
            periodEnd        = Some(java.time.OffsetDateTime.now().minusSeconds(20L)),
            powerConsumption = 0.333
          )

        val testProduceDeviceEvent = OutletDeviceMessage(
          outletId         = fixtureBasicChargingEvent.outletId,
          rfidTag          = testSession.rfidTag,
          periodStart      = testSession.periodStart,
          periodEnd        = testSession.periodEnd.getOrElse(java.time.OffsetDateTime.now()),
          outletStatus     = OutletDeviceState.Charging,
          powerConsumption = testSession.powerConsumption
        )

        val kinesisConsumer = ZIO.serviceWithZIO[KinesisChargingEventsIn](
          _.consume(
            fixtureBasicChargingEvent.copy(
              outletState   = OutletDeviceState.Charging,
              recentSession = testSession
            ))
        )

        val mockOutletService = MockChargerOutletService.SetCharging(
          Assertion.equalTo((fixtureBasicChargingEvent.outletId, testSession.rfidTag)),
          Expectation.unit
        )

        val mockToDevice = MockOutletDeviceMessageProducer
          .Produce(
            Assertion.equalTo(testProduceDeviceEvent),
            Expectation.unit
          )

        for {
          _ <- kinesisConsumer
                .provide(
                  KinesisChargingEventsIn.live,
                  MockChargingEventProducer.empty,
                  mockToDevice,
                  mockOutletService,
                  MockDeadLettersProducer.empty
                )
        } yield assertTrue(true)
      },
      test("handle case AppRequestsStop") {
        val testSession = fixtureBasicChargingEvent
          .recentSession
          .copy(
            sessionId        = Some(UUID.randomUUID()),
            rfidTag          = UUID.randomUUID().toString,
            periodStart      = fixtureBasicChargingEvent.recentSession.periodStart,
            periodEnd        = Some(java.time.OffsetDateTime.now().minusSeconds(20L)),
            powerConsumption = 0.0
          )

        val testConsumeBackendEvent = fixtureBasicChargingEvent.copy(
          outletState   = OutletDeviceState.AppRequestsStop,
          recentSession = testSession
        )

        val testChargerOutlet = new ChargerOutlet(
          outletId              = fixtureBasicChargingEvent.outletId,
          chargerGroupId        = UUID.randomUUID(),
          outletCode            = "String",
          address               = "String",
          maxPower              = 22.0,
          outletState           = OutletDeviceState.ChargingFinished,
          sessionId             = testSession.sessionId,
          rfidTag               = testSession.rfidTag,
          startTime             = testConsumeBackendEvent.recentSession.periodStart,
          endTime               = Some(testSession.periodEnd.get),
          powerConsumption      = 2,
          totalChargingEvents   = 12345L,
          totalPowerConsumption = 1000.0
        )

        val testProduceBackendEvent = ChargingEvent(
          initiator   = EventInitiator.OutletBackend,
          outletId    = fixtureBasicChargingEvent.outletId,
          outletState = testChargerOutlet.outletState,
          recentSession = EventSession(
            sessionId        = testSession.sessionId,
            rfidTag          = testSession.rfidTag,
            periodStart      = testChargerOutlet.startTime,
            periodEnd        = testChargerOutlet.endTime,
            powerConsumption = testChargerOutlet.powerConsumption
          )
        )

        val testProduceDeviceEvent = OutletDeviceMessage(
          outletId         = fixtureBasicChargingEvent.outletId,
          rfidTag          = testSession.rfidTag,
          periodStart      = testSession.periodStart,
          periodEnd        = testSession.periodEnd.getOrElse(java.time.OffsetDateTime.now()),
          outletStatus     = testChargerOutlet.outletState,
          powerConsumption = testSession.powerConsumption
        )

        val kinesisConsumer = ZIO.serviceWithZIO[KinesisChargingEventsIn](_.consume(testConsumeBackendEvent))

        val mockOutletService = MockChargerOutletService.StopCharging(
          Assertion.equalTo(testConsumeBackendEvent),
          Expectation.value(testChargerOutlet)
        )

        val mockToDevice = MockOutletDeviceMessageProducer
          .Produce(
            Assertion.equalTo(testProduceDeviceEvent),
            Expectation.unit
          )

        val mockToBackend = MockChargingEventProducer.Put(
          Assertion.equalTo(testProduceBackendEvent),
          Expectation.unit
        )

        for {
          _ <- kinesisConsumer
                .provide(
                  KinesisChargingEventsIn.live,
                  mockToBackend,
                  mockToDevice,
                  mockOutletService,
                  MockDeadLettersProducer.empty
                )
        } yield assertTrue(true)
      }
    )
}
