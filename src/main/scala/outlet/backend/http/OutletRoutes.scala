package outlet.backend.http

import outlet.backend.ChargerOutletService
import outlet.backend.events.StreamWriter
import outlet.backend.http.dto._
import shared.http.BaseRoutes
import shared.types.outletStatus.OutletStatusEvent
import shared.validation.InputValidation.validateUUID
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.json._

final case class OutletRoutes(service: ChargerOutletService, streamWriter: StreamWriter) extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "chargers" =>
        ZIO.succeed(Response.text("List of chargers in the area..."))

      case req @ Method.POST -> !! / "chargers" / "register" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateChargerOutletDto].orFail(invalidPayload)
          // validate payload
          outlet = dto.toModel
          _ <- service.register(outlet).mapError(serverError)
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              CreateChargerOutletDto.fromModel(outlet).toJson
            }
          )
        }).respond

      case Method.GET -> !! / "chargers" / "outlet" / outlet / "customer" / rfid / "start" =>
        (for {
          outletId <- validateUUID(outlet, "charger").toEither.orFail(unProcessableEntity)
          initData <- service.setChargingRequested(outletId, rfid).mapError(serverError)
          _        <- streamWriter.put(OutletStatusEvent.fromOutlet(initData)).mapError(serverError)
          // customer.backend will consume, check user, then emit ok to us
          // our consumer expects ack and calls service.beginCharging(..) and zio.http client to post respective message to aws api gateway
        } yield {
          Response(Status.Ok, defaultHeaders)
        }).respond

      case Method.GET -> !! / "chargers" / "outlet" / outlet / "customer" / rfid / "stop" =>
        (for {
          outletId <- validateUUID(outlet, "charger").toEither.orFail(unProcessableEntity)
          report   <- service.stopCharging(OutletStatusEvent.deviceStop(outletId, rfid)).mapError(serverError)
          _        <- streamWriter.put(OutletStatusEvent.fromOutlet(report)).mapError(serverError)
          // use zio.http client to post respective message to aws api gateway
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
          report <- service.aggregateConsumption(OutletStatusEvent.fromMidReport(dto)).mapError(serverError)
          _      <- streamWriter.put(OutletStatusEvent.fromOutlet(report)).mapError(serverError)
        } yield {
          Response(Status.Ok, defaultHeaders)
        }).respond
    }

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8080, routes)
}

object OutletRoutes {

  val live: ZLayer[ChargerOutletService with StreamWriter, Nothing, OutletRoutes] =
    ZLayer.fromFunction(OutletRoutes.apply _)
}

/*
  serve rest api    server to listen    to aws api gateway websocket posts from client
  use   simple http client to post back to aws api gateway websocket       to   client


  - post - device has detected start charging { device id, consumer token details }
    - send to kinesis: device detected start charging from device id by consumer token

  - post - device has detected stop charging { device id, consumer token details }
    - send to kinesis: device detected stop charging at device id

  read kinesis
  - consumer backend has requested device id to start charging
    - push to device
    - ack to consumer backend
    - send to kinesis: billing - initiate charging session

  - consumer backend has requests device id to stop charging
    - push to device
    - ack to consumer backend
    - send to kinesis: completed charging session data

  persist in dynamodb
  - chargers
    - status
    - history aggregated to a set of chargers, paginated

 */
