package dev.juhouse.projector.projection2

data class BridgeCaptureDeviceResolution(val width: Int, val height: Int) {
    override fun toString(): String {
        return width.toString() + "x" + height.toString()
    }
}