package dev.juhouse.projector.projection2.time

import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.CanvasDelegate
import dev.juhouse.projector.projection2.Projectable
import dev.juhouse.projector.projection2.text.TextRenderer
import dev.juhouse.projector.projection2.text.TextRendererBounds
import dev.juhouse.projector.utils.FontCreatorUtil
import java.awt.Color
import java.awt.Font
import java.util.*
import kotlin.collections.ArrayList

class ProjectionClock(private val delegate: CanvasDelegate): Projectable {
    private var render: Boolean = false
    private val renderFlag = BridgeRenderFlag(delegate)
    private val textRenders: ArrayList<TextRenderer> = ArrayList()
    private var currentText: String? = null

    private fun getFontFor(render: BridgeRender): Font {
        val newSize = render.width * 0.12f
        return FontCreatorUtil.getRobotoMonoFont().deriveFont(newSize)
    }

    override fun init() {
        renderFlag.property.addListener { _, _, _ ->
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

            render.clearColor = Color(0.0f, 0.0f, 0.0f, 0.5f)
            render.enabled = renderFlag.isRenderEnabled(it.renderId)

            textRenders.add(render)
        }
    }

    override fun setRender(render: Boolean) {
        if (!render) {
            setText(null)
        }

        this.render = render
    }

    override fun getRenderFlag(): BridgeRenderFlag {
        return renderFlag
    }

    fun setText(text: String?) {
        if (!render) {
            return
        }

        if (!renderFlag.hasAnyRender()) {
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