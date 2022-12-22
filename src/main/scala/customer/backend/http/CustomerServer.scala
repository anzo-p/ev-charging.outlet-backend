package customer.backend.http

import zhttp.http.HttpApp
import zhttp.service.Server
import zio.{ZIO, ZLayer}

final case class CustomerServer(customerRoutes: CustomerRoutes, chargingRequestRoutes: ChargingRequestRoutes) {

  val allRoutes: HttpApp[Any, Throwable] =
    customerRoutes.routes ++ chargingRequestRoutes.routes

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8080, allRoutes)
}

object CustomerServer {

  val live: ZLayer[CustomerRoutes with ChargingRequestRoutes, Nothing, CustomerServer] =
    ZLayer.fromFunction(CustomerServer.apply _)
}
