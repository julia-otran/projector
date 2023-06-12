package dev.juhouse.projector.projection2.video

import dev.juhouse.projector.projection2.CanvasDelegate
import dev.juhouse.projector.utils.VlcPlayerFactory
import uk.co.caprica.vlcj.media.discoverer.MediaDiscoverer
import uk.co.caprica.vlcj.media.discoverer.MediaDiscovererCategory
import java.util.Collections

class ProjectionVideoCapture(video: ProjectionVideo, delegate: CanvasDelegate) : ProjectionPlayer(video, delegate) {
    private lateinit var mediaDiscoverer: MediaDiscoverer
    override fun init() {
        super.init()
        val discoverers = VlcPlayerFactory.getFactory().mediaDiscoverers().discoverers(MediaDiscovererCategory.DEVICES)
        mediaDiscoverer = VlcPlayerFactory.getFactory().mediaDiscoverers().discoverer(discoverers[0].name())
        mediaDiscoverer.start()
    }

    override fun finish() {
        mediaDiscoverer.stop()
        super.finish()
    }
    fun getDevices(): List<String> {
        return mediaDiscoverer.newMediaList().media().mrls()
    }
}