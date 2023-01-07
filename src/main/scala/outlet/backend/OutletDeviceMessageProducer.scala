package outlet.backend

import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import zio.Task

trait OutletDeviceMessageProducer {
  def produce(message: OutletDeviceMessage): Task[Unit]
}
