package charger.backend.http

import charger.backend.events.StreamWriter
import charger.backend.http.dto.CreateChargingEvent
import shared.http.BaseRoutes
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.json._

final case class ChargerRoutes(streamWriter: StreamWriter) extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "chargers" =>
        ZIO.succeed(Response.text("List of chargers in the area..."))

      case Method.POST -> !! / "chargers" / charger / "status" / status =>
        ZIO.succeed(Response.text(s"Update charger $charger to status $status..."))

      case req @ Method.POST -> !! / "chargers" / _ / "start" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateChargingEvent].orFail(invalidPayload)
          event = dto.toEvent
          // resolve charger by id
          // resolve user ok - implies exists, validated, and working payment method
          // create <- TodoTaskDto.validate(dto).orFail(invalidPayload) - payload validation must be here
          // service.add(create.toParams).absolve.mapError(serverError) - we could squeeze all dynamodb reads and writes into a single service call
          _ <- streamWriter.put(event).mapError(serverError)
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              CreateChargingEvent.fromEvent(event).toJson
            }
          )
        }).respond

      case _ @Method.POST -> !! / "chargers" / charger / "stop" =>
        ZIO.succeed(Response.text(s"Stop charging for charger $charger..."))
    }

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8080, routes)
}

object ChargerRoutes {

  val live: ZLayer[StreamWriter, Nothing, ChargerRoutes] =
    ZLayer.fromFunction(ChargerRoutes.apply _)
}

/*
  serve rest api

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
