package outlet_backend.events

import outlet_backend.types.chargingEvent.Fixtures.fixtureBasicChargingEvent
import outlet_backend.types.outletDeviceMessage.OutletDeviceMessage
import outlet_backend.utils.mocks.{
  MockChargerOutletService,
  MockChargingEventProducer,
  MockDeadLettersProducer,
  MockOutletDeviceMessageProducer
}
import shared.types.chargingEvent.EventSession
import shared.types.enums.OutletDeviceState
import zio._
import zio.mock.Expectation
import zio.test._

import java.util.UUID

object KinesisChargingEventsInSpec extends ZIOSpecDefault {

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

        val testProduceDeviceMessage = OutletDeviceMessage(
          outletId          = fixtureBasicChargingEvent.outletId,
          rfidTag           = testSession.rfidTag,
          periodStart       = testSession.periodStart,
          periodEnd         = testSession.periodEnd.getOrElse(java.time.OffsetDateTime.now()),
          outletStateChange = OutletDeviceState.Charging,
          powerConsumption  = testSession.powerConsumption
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
            Assertion.equalTo((fixtureBasicChargingEvent.outletId, OutletDeviceState.AppRequestsCharging)),
            Expectation.value(true)
          ) ++
            MockChargerOutletService.SetCharging(
              Assertion.equalTo((fixtureBasicChargingEvent.outletId, testSession.rfidTag, testSession.sessionId.get)),
              Expectation.unit
            )

        val mockToDevice = MockOutletDeviceMessageProducer
          .Produce(
            Assertion.equalTo(testProduceDeviceMessage),
            Expectation.unit
          )

        for {
          _ <- kinesisConsumer
                .provide(
                  KinesisChargingEventsIn.live,
                  mockOutletService,
                  mockToDevice,
                  MockChargingEventProducer.empty,
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

        val testProduceDeviceMessage = OutletDeviceMessage(
          outletId          = fixtureBasicChargingEvent.outletId,
          rfidTag           = testSession.rfidTag,
          periodStart       = testSession.periodStart,
          periodEnd         = testSession.periodEnd.getOrElse(java.time.OffsetDateTime.now()),
          outletStateChange = OutletDeviceState.Charging,
          powerConsumption  = testSession.powerConsumption
        )

        val kinesisConsumer = ZIO.serviceWithZIO[KinesisChargingEventsIn](
          _.consume(
            fixtureBasicChargingEvent.copy(
              outletState   = OutletDeviceState.Charging,
              recentSession = testSession
            ))
        )

        val mockOutletService = MockChargerOutletService.SetCharging(
          Assertion.equalTo((fixtureBasicChargingEvent.outletId, testSession.rfidTag, testSession.sessionId.get)),
          Expectation.unit
        )

        val mockToDevice = MockOutletDeviceMessageProducer
          .Produce(
            Assertion.equalTo(testProduceDeviceMessage),
            Expectation.unit
          )

        for {
          _ <- kinesisConsumer
                .provide(
                  KinesisChargingEventsIn.live,
                  mockOutletService,
                  mockToDevice,
                  MockChargingEventProducer.empty,
                  MockDeadLettersProducer.empty
                )
        } yield assertTrue(true)
      },
      test("handle case AppRequestsStop") {
        val testSession = EventSession(
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

        val testProduceDeviceMessage = OutletDeviceMessage(
          outletId          = fixtureBasicChargingEvent.outletId,
          rfidTag           = testSession.rfidTag,
          periodStart       = testSession.periodStart,
          periodEnd         = testSession.periodEnd.getOrElse(java.time.OffsetDateTime.now()),
          outletStateChange = OutletDeviceState.AppRequestsStop,
          powerConsumption  = testSession.powerConsumption
        )

        val kinesisConsumer = ZIO.serviceWithZIO[KinesisChargingEventsIn](_.consume(testConsumeBackendEvent))

        val mockToDevice = MockOutletDeviceMessageProducer
          .Produce(
            Assertion.equalTo(testProduceDeviceMessage),
            Expectation.unit
          )

        for {
          _ <- kinesisConsumer
                .provide(
                  KinesisChargingEventsIn.live,
                  MockChargerOutletService.empty,
                  mockToDevice,
                  MockChargingEventProducer.empty,
                  MockDeadLettersProducer.empty
                )
        } yield assertTrue(true)
      }
    )
}
