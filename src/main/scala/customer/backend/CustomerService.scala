package customer.backend

import customer.backend.types.customer.Customer
import zio.Task
import zio.dynamodb.Item

import java.util.UUID

trait CustomerService {
  def getById(customerId: UUID): Task[Customer]

  def getRfidTag(customerId: UUID): Task[String]

  def register(customer: Customer): Task[Customer]

  def update(customerId: UUID, params: Customer): Task[Option[Item]]
}
