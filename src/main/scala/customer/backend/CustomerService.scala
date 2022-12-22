package customer.backend

import customer.backend.types.{Customer, CustomerParams}
import zio.IO
import zio.dynamodb.Item

import java.util.UUID

trait CustomerService {
  def getById(customerId: UUID): IO[Throwable, Customer]

  def register(params: CustomerParams): IO[Throwable, Customer]

  def update(customerId: UUID, params: CustomerParams): IO[Throwable, Option[Item]]
}
