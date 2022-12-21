package consumer.backend.http

import consumer.backend.events.StreamWriter
import consumer.backend.http.dto.CreateChargingSession
import shared.http.BaseRoutes
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.json.{DecoderOps, EncoderOps}

final case class ConsumerRoutes(streamWriter: StreamWriter) extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "chargers" / "start" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateChargingSession].orFail(invalidPayload)
          event = dto.toEvent
          // resolve charger by id
          // resolve user ok - implies exists, validated, and working payment method
          // create <- TodoTaskDto.validate(dto).orFail(invalidPayload) - payload validation must be here
          // service.add(create.toParams).absolve.mapError(serverError) - we could squeeze all dynamodb reads and writes into a single service call
          _ <- streamWriter.put(event).mapError(serverError)
          // response created
          // mobile app will then query other endpoints for more data
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              CreateChargingSession.fromEvent(event).toJson
            }
          )
        }).respond

      case _ @Method.POST -> !! / "chargers" / charger / "stop" =>
        ZIO.succeed(Response.text(s"Stop charging for charger $charger..."))
    }

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8080, routes)
}

object ConsumerRoutes {

  val live: ZLayer[StreamWriter, Nothing, ConsumerRoutes] =
    ZLayer.fromFunction(ConsumerRoutes.apply _)
}
/*
  serve rest api
  - post - consumer client requests begins charging { consumer data }
    - send to kinesis: consumer requests start charging from device id

  - post - consumer client requests stops charging { session id or consumer data }
    - send to kinesis: consumer requests stop charging at device id

  - get consumer clients charging history

  read kinesis
  - charger backend has issued to start charging
    - push to device

  - charger backend has issued a stop charging
    - push to device- push to device

  persist in dynamodb
  - consumer
    - active events
    - history, paginated
    - tally of charging and expenses
 */
