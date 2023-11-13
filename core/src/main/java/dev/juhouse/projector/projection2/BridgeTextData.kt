package dev.juhouse.projector.projection2

data class BridgeTextData(
    val renderId: Int,
    val imageData: IntArray,
    val positionX: Int,
    val positionY: Int,
    val imageWidth: Int,
    val imageHeight: Int,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val darkenBackground: Boolean
)
