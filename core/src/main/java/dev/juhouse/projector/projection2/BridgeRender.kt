package dev.juhouse.projector.projection2

data class BridgeRender(
    val renderId: Int,
    val renderName: String,
    val enableRenderBackgroundAssets: Boolean,
    val enableRenderImage: Boolean,
    val enableRenderVideo: Boolean
)
