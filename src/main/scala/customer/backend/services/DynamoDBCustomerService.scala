package customer.backend.services

import customer.backend.CustomerService
import customer.backend.types.customer.Customer
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb.ProjectionExpression.$
import zio.dynamodb._

import java.util.UUID

final case class DynamoDBCustomerService(executor: DynamoDBExecutor) extends CustomerService {
  import DynamoDBCustomerService.tableResource

  override def getById(customerId: UUID): IO[Throwable, Customer] =
    (for {
      query <- get[Customer](tableResource, PrimaryKey("customerId" -> customerId.toString)).execute
    } yield query)
      .flatMap {
        case Left(error)  => ZIO.fail(new Throwable(error))
        case Right(value) => ZIO.succeed(value)
      }
      .provideLayer(ZLayer.succeed(executor))

  override def register(customer: Customer): IO[Throwable, Customer] =
    (for {
      insert <- ZIO.succeed(customer)
      _      <- put(tableResource, insert).execute
    } yield insert)
      .provideLayer(ZLayer.succeed(executor))

  override def update(customerId: UUID, customer: Customer): IO[Throwable, Option[Item]] =
    updateItem(tableResource, PrimaryKey("customerId" -> customerId.toString)) {
      $("address").set(customer.address) +
        $("email").set(customer.email) +
        $("paymentMethod").set(customer.paymentMethod)
    }.execute
      .provideLayer(ZLayer.succeed(executor))
}

object DynamoDBCustomerService {

  val tableResource = "ev-outlet-app.customer.table"

  val live: ZLayer[DynamoDBExecutor, Nothing, CustomerService] =
    ZLayer.fromFunction(DynamoDBCustomerService.apply _)
}
