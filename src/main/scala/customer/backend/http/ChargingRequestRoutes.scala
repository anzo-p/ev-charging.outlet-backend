package customer.backend.http

import customer.backend.events.StreamWriter
import customer.backend.http.dto.{ChargingSessionDto, CreateChargingSessionDto}
import customer.backend.{ChargingService, CustomerService}
import shared.http.BaseRoutes
import shared.types.outletStatus.OutletStatusEvent
import shared.validation.InputValidation.validateUUID
import zhttp.http._
import zio._
import zio.json.{DecoderOps, EncoderOps}

final case class ChargingRequestRoutes(customerService: CustomerService, chargingService: ChargingService, outletProducer: StreamWriter)
    extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "chargers" / "customer" / customer / "history" =>
        (for {
          customerId <- validateUUID(customer, "customer").toEither.orFail(unProcessableEntity)
          history    <- chargingService.getHistory(customerId).mapError(serverError)
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              history.map(ChargingSessionDto.fromModel).toJson
            }
          )
        }).respond

      case req @ Method.POST -> !! / "chargers" / "start" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateChargingSessionDto].orFail(invalidPayload)
          // create <- CreateChargingSession.validate(dto).orFail(invalidPayload) - nothing to validate yet
          rfidTag <- customerService.getRfidTag(dto.customerId).orElseFail(invalidPayload("this customer doesn't exist"))
          session = dto.toModel
          _ <- chargingService.initialize(session).mapError(serverError)
          _ <- outletProducer.put(OutletStatusEvent.appStart(session.outletId, rfidTag)).mapError(serverError)
          // app will forward to poll for status reports
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              ChargingSessionDto.fromModel(session).toJson
            }
          )
        }).respond

      case req @ Method.POST -> !! / "chargers" / "stop" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[ChargingSessionDto].orFail(invalidPayload)
          // validate
          rfidTag <- customerService.getRfidTag(dto.customerId).orElseFail(invalidPayload("this customer doesn't exist"))
          _       <- chargingService.setStopRequested(dto.sessionId).mapError(serverError)
          _       <- outletProducer.put(OutletStatusEvent.appStop(dto.outletId, rfidTag)).mapError(serverError)
          // app will forward to poll for final report
        } yield {
          Response(Status.Ok, defaultHeaders)
        }).respond
    }
}

object ChargingRequestRoutes {

  val live: ZLayer[StreamWriter with CustomerService with ChargingService, Nothing, ChargingRequestRoutes] =
    ZLayer.fromFunction(ChargingRequestRoutes.apply _)
}
