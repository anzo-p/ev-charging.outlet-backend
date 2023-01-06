package outlet.backend.types.chargerOutlet

import shared.types.enums.OutletDeviceState
import shared.types.enums.OutletDeviceState.getPreStatesTo

object ChargerOutletOps {
  implicit class ChargerOutletOps(outlet: ChargerOutlet) {

    def mayTransitionTo(targetState: OutletDeviceState): Boolean =
      outlet.outletState.in(getPreStatesTo(targetState))
  }
}
