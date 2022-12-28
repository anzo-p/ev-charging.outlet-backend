package app.backend.http

import app.backend.{ChargingService, CustomerService}
import app.backend.events.StreamWriter
import app.backend.http.dto.{ChargingSessionDto, CreateChargingSessionDto}
import shared.http.BaseRoutes
import shared.types.outletStatus.OutletStatusEvent
import shared.validation.InputValidation._
import zhttp.http._
import zio._
import zio.json.{DecoderOps, EncoderOps}

final case class ChargingRoutes(customerService: CustomerService, chargingService: ChargingService, outletProducer: StreamWriter)
    extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "chargers" / "customer" / customer / "history" =>
        (for {
          customerId <- validateUUID(customer, "customer").toEither.orFail(unProcessableEntity)
          history    <- chargingService.getHistory(customerId).mapError(th => badRequest(th.getMessage))
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
          _ <- chargingService.initialize(session).mapError(th => badRequest(th.getMessage))
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

      case Method.GET -> !! / "chargers" / "customer" / customer / "session" / session =>
        (for {
          urlVars <- (validateUUID(customer, "customer") <*> validateUUID(session, "session")).combineErrors.orFail(unProcessableEntity)
          (customerId, sessionId) = urlVars
          _       <- customerService.getRfidTag(customerId).orElseFail(invalidPayload("this customer doesn't exist"))
          session <- chargingService.getSession(sessionId).mapError(th => badRequest(th.getMessage))
        } yield {
          Response(
            Status.Ok,
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
          _       <- chargingService.setStopRequested(dto.sessionId).mapError(th => badRequest(th.getMessage))
          _       <- outletProducer.put(OutletStatusEvent.appStop(dto.outletId, rfidTag)).mapError(serverError)
          // app will forward to poll for final report
        } yield {
          Response(Status.Ok, defaultHeaders)
        }).respond
    }
}

object ChargingRoutes {

  val live: ZLayer[StreamWriter with CustomerService with ChargingService, Nothing, ChargingRoutes] =
    ZLayer.fromFunction(ChargingRoutes.apply _)
}
