package dev.juhouse.projector.projection2

interface BridgeNDIDeviceFindCallback {
    fun onDevicesChanged(devices: Array<BridgeNDIDevice>)
}
