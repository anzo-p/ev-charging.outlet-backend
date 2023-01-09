package outlet.backend.http

import outlet.backend.http.dto.ChargerOutletDto
import outlet.backend.mocks.StubChargerOutletService
import outlet.backend.types.chargerOutlet.ChargerOutletSpec.{testChargerOutlet, testChargerOutletDto}
import zhttp.http._
import zio._
import zio.json.{DecoderOps, EncoderOps}
import zio.test._

object OutletRoutesSpec extends ZIOSpecDefault {

  def spec =
    suite("OutletRoutes")(
      test("GET /chargers") {
        for {
          uri      <- ZIO.fromEither(URL.fromString("http://127.0.0.1:8081/chargers"))
          request  <- ZIO.from(Request(url = uri))
          response <- OutletRoutes(StubChargerOutletService).routes(request)
          body     <- response.body.asString
          expected <- Body.fromString("List of chargers in the area...").asString
        } yield assertTrue(response.status == Status.Ok && body == expected)
      },
      test("POST /chargers/register") {
        for {
          uri      <- ZIO.fromEither(URL.fromString("http://127.0.0.1:8081/chargers/register"))
          request  <- ZIO.from(Request(url = uri, method = Method.POST, body = Body.fromString(testChargerOutletDto.toJson)))
          response <- OutletRoutes(StubChargerOutletService).routes(request)
          body     <- response.body.asString
          outlet   <- ZIO.fromEither(body.fromJson[ChargerOutletDto])
          expected <- Body.fromString(ChargerOutletDto.fromModel(testChargerOutlet).copy(outletId = outlet.outletId).toJson).asString
        } yield assertTrue(response.status == Status.Created && outlet.toJson == expected)
      },
      test("POST /chargers/register responds bad request if payload violates") {
        for {
          uri      <- ZIO.fromEither(URL.fromString("http://127.0.0.1:8081/chargers/register"))
          request  <- ZIO.from(Request(url = uri, method = Method.POST, body = Body.fromString("this payload will fail")))
          response <- OutletRoutes(StubChargerOutletService).routes(request)
        } yield assertTrue(response.status == Status.BadRequest)
      }
    )
}
