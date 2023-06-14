package dev.juhouse.projector.projection2.video

import dev.juhouse.projector.projection2.CanvasDelegate
import java.util.*

class ProjectionVideoCapture(video: ProjectionVideo, val delegate: CanvasDelegate) : ProjectionPlayer(video, delegate) {
    fun getDevices(): List<String> {
        return delegate.bridge.videoCaptureDevices?.asList() ?: Collections.emptyList()
    }
}