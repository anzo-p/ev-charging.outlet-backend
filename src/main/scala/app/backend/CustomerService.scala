package app.backend

import app.backend.types.customer.Customer
import zio.Task
import zio.dynamodb.Item

import java.util.UUID

trait CustomerService {
  def register(customer: Customer): Task[Customer]

  def getById(customerId: UUID): Task[Customer]

  def getRfidTag(customerId: UUID): Task[String]

  def getCustomerByRfidTag(rfidTag: String): Task[Option[UUID]]

  def update(customerId: UUID, params: Customer): Task[Option[Item]]
}
