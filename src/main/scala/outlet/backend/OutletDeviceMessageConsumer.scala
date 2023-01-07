package outlet.backend

import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import zio.Task

trait OutletDeviceMessageConsumer {
  def consume(msg: OutletDeviceMessage): Task[Unit]
}
