package outlet_backend

import outlet_backend.types.outletDeviceMessage.OutletDeviceMessage
import zio.Task

trait OutletDeviceMessageConsumer {
  def consume(msg: OutletDeviceMessage): Task[Unit]
}
