package dev.juhouse.projector.projection2.video

import dev.juhouse.projector.projection2.BridgeCaptureDevice
import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.CanvasDelegate
import java.util.*

class ProjectionVideoCapture(val video: ProjectionVideo, val delegate: CanvasDelegate) : ProjectionPlayer(video, delegate) {
    fun getDevices(): List<BridgeCaptureDevice> {
        return delegate.bridge.videoCaptureDevices?.asList() ?: Collections.emptyList()
    }
    override fun applyRenderFlag() {
        video.renderFlagProperty.get().applyDefault(BridgeRender::enableRenderBackgroundAssets)
    }
}