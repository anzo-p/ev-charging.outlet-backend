package customer.backend.http.dto

import customer.backend.types._
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class CreateCustomer(
    customerId: Option[UUID],
    rfidTag: Option[String],
    address: String,
    email: String,
    paymentMethod: String
  ) {

  def toParams: CustomerParams =
    CustomerParams(
      address       = this.address,
      email         = this.email,
      paymentMethod = this.paymentMethod
    )
}

object CreateCustomer {
  implicit val codec: JsonCodec[CreateCustomer] = DeriveJsonCodec.gen[CreateCustomer]

  def fromModel(model: Customer): CreateCustomer =
    CreateCustomer(
      customerId    = Some(model.customerId),
      rfidTag       = Some(model.rfidTag),
      address       = model.address,
      email         = model.email,
      paymentMethod = model.paymentMethod
    )
}
