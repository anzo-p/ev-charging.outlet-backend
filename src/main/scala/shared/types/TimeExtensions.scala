package shared.types

import zio.schema.Schema.Field
import zio.schema.{Schema, StandardType}

import java.time.format.DateTimeFormatter

object TimeExtensions {

  trait DateTimeSchemaImplicits {
    implicit val offsetDateTimeSchema: Schema[java.time.OffsetDateTime] =
      Schema.Primitive(StandardType.OffsetDateTimeType(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

    implicit val offsetDateTimeField: Field[java.time.OffsetDateTime] =
      Schema.Field[java.time.OffsetDateTime]("offsetDateTimeField", offsetDateTimeSchema)
  }

  implicit class JavaOffsetDateTimeOps(val ts: java.time.OffsetDateTime) extends AnyVal {

    def toProtobufTs: com.google.protobuf.timestamp.Timestamp =
      com.google.protobuf.timestamp.Timestamp {
        ts.toInstant
      }
  }

  implicit class ProtobufTimestampOps(val ts: com.google.protobuf.timestamp.Timestamp) extends AnyVal {

    def toJavaOffsetDateTime: java.time.OffsetDateTime =
      java
        .time
        .OffsetDateTime
        .ofInstant(
          java.time.Instant.ofEpochMilli(ts.seconds * 1000 + ts.nanos / 1000000),
          java.time.ZoneOffset.UTC
        )
  }
}
