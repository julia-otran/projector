package dev.juhouse.projector.projection2.countdown

import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.CanvasDelegate
import dev.juhouse.projector.projection2.Projectable
import dev.juhouse.projector.projection2.text.TextRenderer
import dev.juhouse.projector.projection2.text.TextRendererBounds
import dev.juhouse.projector.projection2.video.ProjectionBackgroundVideo
import dev.juhouse.projector.projection2.video.ProjectionVideo
import java.awt.Font
import java.util.Collections

class ProjectionCountdown(private val delegate: CanvasDelegate): Projectable {
    private val backgroundVideo = ProjectionBackgroundVideo(ProjectionVideo(delegate))
    private val textRenders: ArrayList<TextRenderer> = ArrayList()
    private var render = false
    private var prevText = ""

    private fun getFontFor(render: BridgeRender): Font {
        val scaleX = render.width / delegate.mainWidth.toFloat()
        val scaleY = render.height / delegate.mainHeight.toFloat()

        val newSize = delegate.fontProperty.value.size * scaleX.coerceAtMost(scaleY)
        return delegate.fontProperty.value.deriveFont(newSize)
    }

    override fun init() {
        backgroundVideo.init()
    }

    override fun finish() {
        backgroundVideo.finish()
    }

    override fun rebuild() {
        backgroundVideo.rebuild()

        textRenders.clear()

        delegate.bridge.renderSettings.forEach {
            val bounds = TextRendererBounds(
                it.renderId,
                0,
                0,
                it.width,
                it.height
            )

            val render = TextRenderer(bounds, getFontFor(it))
            textRenders.add(render)
        }
    }

    override fun setRender(render: Boolean) {
        if (render) {
            backgroundVideo.setRender(true)
            backgroundVideo.startBackground(0, null)
        } else {
            backgroundVideo.stopBackground()
            backgroundVideo.setRender(false)
            setText(null)
        }

        this.render = render
    }

    fun setText(text: String?) {
        if (!render) {
            return
        }

        if (prevText == text) {
            return;
        }

        if (text == null) {
            prevText = ""
            delegate.bridge.setTextData(null)
        } else {
            prevText = text
            delegate.bridge.setTextData(textRenders.map { it.renderText(Collections.singletonList(text)) }.toTypedArray())
        }
    }
}