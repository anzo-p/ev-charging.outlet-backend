package shared.types.deadLetters

import nl.vroste.zio.kinesis.client.Record
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class DeadLetterMessage(
    partitionKey: String,
    data: String,
    error: String,
    stackTrace: String
  )

object DeadLetterMessage {
  implicit val codec: JsonCodec[DeadLetterMessage] =
    DeriveJsonCodec.gen[DeadLetterMessage]

  def make[T](rec: Record[T], th: Throwable): DeadLetterMessage =
    DeadLetterMessage(
      partitionKey = rec.partitionKey,
      data         = rec.data.toString,
      error        = th.toString,
      stackTrace   = th.getStackTrace.toList.map(_.toString).mkString("\n")
    )
}
