package dev.juhouse.projector.projection2.time

import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.CanvasDelegate
import dev.juhouse.projector.projection2.Projectable
import dev.juhouse.projector.projection2.text.TextRenderer
import dev.juhouse.projector.projection2.text.TextRendererBounds
import dev.juhouse.projector.projection2.video.ProjectionBackgroundVideo
import dev.juhouse.projector.projection2.video.ProjectionVideo
import javafx.beans.property.ReadOnlyObjectProperty
import java.awt.Font
import java.util.Collections

class ProjectionCountdown(private val delegate: CanvasDelegate): Projectable {
    private val backgroundVideo = ProjectionBackgroundVideo(ProjectionVideo(delegate))
    private val textRenders: ArrayList<TextRenderer> = ArrayList()
    private var render = false
    private var currentText: String? = null

    private fun getFontFor(render: BridgeRender): Font {
        val scaleX = render.width / delegate.mainWidth.toFloat()
        val scaleY = render.height / delegate.mainHeight.toFloat()

        val newSize = delegate.fontProperty.value.size * scaleX.coerceAtMost(scaleY)
        return delegate.fontProperty.value.deriveFont(newSize)
    }

    override fun init() {
        backgroundVideo.init()

        renderFlag.flagValueProperty.addListener { _, _, _ ->
            var changed = false

            textRenders.forEach {
                val enable = renderFlag.isRenderEnabled(it.bounds.renderId)

                if (it.enabled != enable) {
                    it.enabled = enable
                    changed = true
                }
            }

            if (changed) {
                doRender()
            }
        }
    }

    override fun finish() {
        backgroundVideo.finish()
    }

    override fun rebuild() {
        textRenders.clear()

        delegate.bridge.renderSettings.forEach {
            val bounds = TextRendererBounds(
                it.renderId,
                0,
                0,
                it.width,
                it.height,
                false
            )

            val render = TextRenderer(bounds, getFontFor(it))

            render.enabled = renderFlag.isRenderEnabled(it.renderId)

            textRenders.add(render)
        }

        backgroundVideo.rebuild()
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

    override fun getRenderFlag(): BridgeRenderFlag {
        return backgroundVideo.renderFlag
    }

    fun setText(text: String?) {
        if (!render) {
            return
        }

        if (currentText != null && currentText == text) {
            return
        }

        currentText = text
        doRender()
    }

    private fun doRender() {
        currentText?.let { text ->
            delegate.bridge.setTextData(textRenders.map { it.renderText(Collections.singletonList(text)) }.toTypedArray())
        } ?: delegate.bridge.setTextData(null)
    }
}