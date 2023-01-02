package outlet.backend.http

import outlet.backend.ChargerOutletService
import outlet.backend.http.dto._
import shared.events.OutletEventProducer
import shared.http.BaseRoutes
import shared.types.outletStatus.OutletStatusEvent
import shared.validation.InputValidation.validateUUID
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.json._

final case class OutletRoutes(service: ChargerOutletService, streamWriter: OutletEventProducer) extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "chargers" =>
        ZIO.succeed(Response.text("List of chargers in the area..."))

      case req @ Method.POST -> !! / "chargers" / "register" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[ChargerOutletDto].orFail(invalidPayload)
          // validate payload
          outlet = dto.toModel
          _ <- service.register(outlet).mapError(th => badRequest(th.getMessage))
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              ChargerOutletDto.fromModel(outlet).toJson
            }
          )
        }).respond

      case Method.GET -> !! / "chargers" / "outlet" / outlet / "customer" / rfid / "start" =>
        (for {
          outletId <- validateUUID(outlet, "charger").toEither.orFail(unProcessableEntity)
          initData <- service.setChargingRequested(OutletStatusEvent.deviceStart(outletId, rfid)).mapError(th => badRequest(th.getMessage))
          _        <- streamWriter.put(initData.toOutletStatus).mapError(serverError)
          // customer.backend will consume, check user, then emit ok to us
          // our consumer expects ack and calls service.beginCharging(..) and zio.http client to post respective message to aws api gateway
        } yield {
          Response(Status.Ok, defaultHeaders)
        }).respond

      case req @ Method.POST -> !! / "chargers" / _ / "customer" / _ / "progress" =>
        (for {
          //outlet <- validateUUID(outlet, "charger").toEither.orFail(unProcessableEntity)
          // why would we ever post data in url and payload?
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateIntermediateReport].orFail(invalidPayload)
          // validate input
          report <- service.aggregateConsumption(CreateIntermediateReport.toOutletStatus(dto)).mapError(th => badRequest(th.getMessage))
          _      <- streamWriter.put(report.toOutletStatus).mapError(serverError)
        } yield {
          Response(Status.Ok, defaultHeaders)
        }).respond

      case Method.GET -> !! / "chargers" / "outlet" / outlet / "customer" / rfid / "stop" =>
        (for {
          outletId <- validateUUID(outlet, "charger").toEither.orFail(unProcessableEntity)
          report   <- service.stopCharging(OutletStatusEvent.deviceStop(outletId, rfid)).mapError(th => badRequest(th.getMessage))
          _        <- streamWriter.put(report.toOutletStatus).mapError(serverError)
          // use zio.http client to post respective message to aws api gateway
        } yield {
          Response(Status.Ok, defaultHeaders)
        }).respond
    }

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8081, routes)
}

object OutletRoutes {

  val live: ZLayer[ChargerOutletService with OutletEventProducer, Nothing, OutletRoutes] =
    ZLayer.fromFunction(OutletRoutes.apply _)
}
