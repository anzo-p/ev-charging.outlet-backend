package outlet.backend.http

import outlet.backend.ChargerOutletService
import outlet.backend.http.dto._
import shared.events.ChargingEventProducer
import shared.http.BaseRoutes
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.json._

final case class OutletRoutes(service: ChargerOutletService) extends BaseRoutes {

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
    }

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8081, routes)
}

object OutletRoutes {

  val live: ZLayer[ChargerOutletService with ChargingEventProducer, Nothing, OutletRoutes] =
    ZLayer.fromFunction(OutletRoutes.apply _)
}
