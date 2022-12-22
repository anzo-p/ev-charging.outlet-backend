package customer.backend.types

import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class Customer(
    customerId: UUID,
    rfidTag: String,
    address: String,
    email: String,
    paymentMethod: String
  )

object Customer {
  implicit lazy val schema: Schema[Customer] = DeriveSchema.gen[Customer]

  def from(params: CustomerParams): Customer =
    Customer(
      customerId    = UUID.randomUUID(),
      rfidTag       = UUID.randomUUID().toString,
      address       = params.address,
      email         = params.email,
      paymentMethod = params.paymentMethod
    )
}
