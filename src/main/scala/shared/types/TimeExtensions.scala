package shared.types

object TimeExtensions {
  implicit class JavaOffsetDateTimeOps(val ts: java.time.OffsetDateTime) extends AnyVal {

    def toProtobufTs: com.google.protobuf.timestamp.Timestamp =
      com.google.protobuf.timestamp.Timestamp {
        ts.toInstant
      }
  }

  implicit class ProtobufTimestampOps(val ts: com.google.protobuf.timestamp.Timestamp) extends AnyVal {

    def toJavaOffsetDateTime: java.time.OffsetDateTime = {
      println(s"from protobuf $ts")
      java
        .time
        .OffsetDateTime
        .ofInstant(
          java.time.Instant.ofEpochMilli(ts.seconds * 1000 + ts.nanos / 1000000),
          java.time.ZoneOffset.UTC
        )
    }
  }
}
