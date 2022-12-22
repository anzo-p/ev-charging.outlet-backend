package customer.backend.service

import customer.backend.CustomerService
import customer.backend.types.{Customer, CustomerParams}
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb.ProjectionExpression.$
import zio.dynamodb._

import java.util.UUID

final case class DynamoDBCustomerService(executor: DynamoDBExecutor) extends CustomerService {
  import customer.backend.service.DynamoDBCustomerService.tableResource

  override def getById(customerId: UUID): IO[Throwable, Customer] =
    (for {
      query <- get[Customer](tableResource, PrimaryKey("customerId" -> customerId.toString)).execute
    } yield query)
      .flatMap {
        case Left(error)  => ZIO.fail(new Throwable(error))
        case Right(value) => ZIO.succeed(value)
      }
      .provideLayer(ZLayer.succeed(executor))

  override def register(params: CustomerParams): IO[Throwable, Customer] =
    (for {
      customer <- ZIO.succeed(Customer.from(params))
      _        <- put(tableResource, customer).execute
    } yield customer)
      .provideLayer(ZLayer.succeed(executor))

  override def update(customerId: UUID, params: CustomerParams): IO[Throwable, Option[Item]] =
    updateItem(tableResource, PrimaryKey("customerId" -> customerId.toString)) {
      $("address").set(params.address) +
        $("email").set(params.email) +
        $("paymentMethod").set(params.paymentMethod)
    }.execute
      .provideLayer(ZLayer.succeed(executor))
}

object DynamoDBCustomerService {

  val tableResource = "ev-outlet-app.customer.table"

  val live: ZLayer[DynamoDBExecutor, Nothing, CustomerService] =
    ZLayer.fromFunction(DynamoDBCustomerService.apply _)
}
