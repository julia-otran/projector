package dev.juhouse.projector.projection2

data class BridgeRender(
    val renderId: Int,
    val renderName: String,
    val width: Int,
    val height: Int,
    val textAreaX: Int,
    val textAreaY: Int,
    val textAreaWidth: Int,
    val textAreaHeight: Int,
    val enableRenderBackgroundAssets: Boolean,
    val enableRenderImage: Boolean,
    val enableRenderVideo: Boolean,
    val renderMode: Int
)
