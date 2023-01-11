package outlet_backend

import outlet_backend.types.outletDeviceMessage.OutletDeviceMessage
import zio.Task

trait OutletDeviceMessageProducer {
  def produce(message: OutletDeviceMessage): Task[Unit]
}
