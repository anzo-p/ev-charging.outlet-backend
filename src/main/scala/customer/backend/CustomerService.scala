package customer.backend

import customer.backend.types.customer.Customer
import zio.IO
import zio.dynamodb.Item

import java.util.UUID

trait CustomerService {
  def getById(customerId: UUID): IO[Throwable, Customer]

  def register(customer: Customer): IO[Throwable, Customer]

  def update(customerId: UUID, params: Customer): IO[Throwable, Option[Item]]
}
