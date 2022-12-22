package customer.backend.http

import customer.backend.events.StreamWriter
import customer.backend.http.dto.CreateChargingSession
import customer.backend.service.{ChargingService, CustomerService}
import shared.http.BaseRoutes
import zhttp.http._
import zio._
import zio.json.{DecoderOps, EncoderOps}

final case class ChargingRequestRoutes(streamWriter: StreamWriter, customerService: CustomerService, chargingService: ChargingService)
    extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "chargers" / "start" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateChargingSession].orFail(invalidPayload)
          // create <- CreateChargingSession.validate(dto).orFail(invalidPayload) - nothing to validate yet
          event = dto.toEvent
          _       <- customerService.getById(dto.customerId).orElseFail(invalidPayload("this customer doesn't exist"))
          already <- chargingService.hasActiveSession(dto.customerId).mapError(serverError)
          _       <- ZIO.fromEither(Either.cond(!already, (), badRequest("customer already has active session")))
          _       <- chargingService.add(event).mapError(serverError)
          _       <- streamWriter.put(event).mapError(serverError)
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
}

object ChargingRequestRoutes {

  val live: ZLayer[StreamWriter with CustomerService with ChargingService, Nothing, ChargingRequestRoutes] =
    ZLayer.fromFunction(ChargingRequestRoutes.apply _)
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
