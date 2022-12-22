package shared.validation

import zio.prelude.Validation

import java.util.UUID

object InputValidation {

  implicit class ValidationOps[A](results: Validation[ValidationError, A]) {

    def combineErrors: Either[String, A] =
      results.fold(
        errors => Left(errors.toList.map(_.message).mkString(", ")),
        value => Right(value)
      )
  }

  def validateUUID(value: String, field: String): Validation[ValidationError, UUID] =
    if (value.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
      Validation.succeed(UUID.fromString(value))
    }
    else {
      Validation.fail(ValidationError(s"$field is not in UUID format"))
    }

  def validateNonEmpty(value: String, field: String): Validation[ValidationError, String] =
    if (value.nonEmpty) {
      Validation.succeed(value)
    }
    else {
      Validation.fail(ValidationError(s"$field cannot be empty"))
    }

  def validatePositiveInt(value: Option[Int], field: String): Validation[ValidationError, Option[Int]] =
    value match {
      case None =>
        Validation.succeed(value)

      case Some(k) =>
        if (k > 0) {
          Validation.succeed(value)
        }
        else {
          Validation.fail(ValidationError(s"$field must be positive"))
        }
    }
}
