package shared.http

import zhttp.http._
import zio._

trait BaseRoutes {

  implicit class EitherOps[E, A](a: Either[E, A]) {

    def orFail(errorHandler: E => Response): IO[Response, A] =
      ZIO.fromEither(a.left.map(errorHandler))
  }

  implicit class EitherWayResponseOps(z: IO[Response, Response]) {

    def respond: ZIO[Any, Throwable, Response] =
      z.foldZIO(
        ZIO.succeed(_),
        ZIO.succeed(_)
      )
  }

  val defaultHeaders: Headers =
    Headers
      .empty
      .addHeaders(Headers.accept(HeaderValues.applicationJson))
      .addHeaders(Headers.contentType(HeaderValues.applicationJson))
      .addHeaders(Headers.accessControlAllowMethods(Method.POST, Method.GET))

  def serverError(th: Throwable): Response = {
    // log error
    println(s"server error: ${th.getMessage}")
    Response(Status.InternalServerError, Headers.empty, Body.empty)
  }

  def badRequest(reason: String): Response =
    Response(Status.BadRequest, Headers.empty, Body.fromString(reason))

  def unProcessableEntity(reason: String): Response = {
    //println(reason)
    Response(Status.UnprocessableEntity, Headers.empty, Body.fromString(reason))
  }

  def invalidPayload(reason: String): Response =
    badRequest("invalid payload " + reason)
}
