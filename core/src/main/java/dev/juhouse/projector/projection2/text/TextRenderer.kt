package dev.juhouse.projector.projection2.text

import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.BridgeTextData
import dev.juhouse.projector.projection2.models.StringWithPosition
import java.awt.*
import java.awt.font.GlyphVector
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.util.function.Consumer

data class TextRendererBounds(val renderId: Int, val x: Int, val y: Int, val w: Int, val h: Int)

class TextRenderer(val bounds: TextRendererBounds, var font: Font) {
    private val image: BufferedImage = BufferedImage(bounds.w, bounds.h, BufferedImage.TYPE_INT_ARGB)

    private val clearColor = Color(0, 0, 0,0)
    private val graphics2D: Graphics2D = image.createGraphics()

    var enabled: Boolean = true

    private val fontMetrics: FontMetrics get() {
        return graphics2D.getFontMetrics(font)
    }

    val textWrapperMetrics: TextWrapperMetrics get() {
        return TextWrapperMetrics(bounds.renderId, fontMetrics, bounds.w, bounds.h)
    }

    fun renderText(text: List<String>): BridgeTextData {
        clearImage()

        if (!enabled) {
            return BridgeTextData(
                    bounds.renderId,
                    (image.data.dataBuffer as DataBufferInt).data,
                    bounds.x,
                    bounds.y,
                    image.width,
                    image.height,
                    bounds.x.toDouble(),
                    bounds.y.toDouble(),
                    bounds.w.toDouble(),
                    bounds.h.toDouble()
            )
        }

        val linePositions = generateLinePositions(text)
        printTextOnImage(linePositions)

        return BridgeTextData(
                    bounds.renderId,
                    (image.data.dataBuffer as DataBufferInt).data,
                    bounds.x,
                    bounds.y,
                    image.width,
                    image.height,
                    linePositions.maxOf { it.x }.toDouble(),
                    linePositions.maxOf { it.y }.toDouble(),
                    linePositions.maxOf { it.w }.toDouble(),
                    linePositions.maxOf { it.y + it.h }.toDouble()
        )
    }

    private fun clearImage() {
        val g: Graphics2D = graphics2D

        val oldComposite = g.composite
        g.composite = AlphaComposite.Clear
        g.color = clearColor
        g.fillRect(0, 0, bounds.w, bounds.h)
        g.composite = oldComposite
    }

    private fun printTextOnImage(drawLines: List<StringWithPosition>) {
        val g: Graphics2D = graphics2D

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        g.stroke = BasicStroke(3f + font.size * 0.025f)

        val baseTransform = g.transform

        drawLines.forEach(Consumer { pt: StringWithPosition ->
            g.translate(pt.x, pt.y)

            // create a glyph vector from your text
            val glyphVectorOutline: GlyphVector = font.createGlyphVector(g.fontRenderContext, pt.text)

            val textShape = glyphVectorOutline.outline
            g.color = Color.black
            g.draw(textShape)
            g.color = Color.white
            g.fill(textShape)
            g.transform = baseTransform
        })
    }

    private fun generateLinePositions(lines: List<String>): List<StringWithPosition> {
        if (lines.isEmpty()) {
            return emptyList()
        }

        var lineCount = lines.size

        val fontHeight = fontMetrics.ascent
        val between = fontMetrics.leading + fontMetrics.descent

        val totalHeight = fontHeight * lines.size + between * (lineCount - 1)

        val emptyHeight: Int = bounds.h  - totalHeight

        var translateY = fontHeight + emptyHeight / 2
        val width: Int = bounds.w

        val pendingLines: MutableList<StringWithPosition> = ArrayList()

        for (line in lines) {
            val lineWidth = fontMetrics.stringWidth(line)

            val x = (width - lineWidth) / 2
            val y = translateY

            translateY += fontHeight

            lineCount--

            if (lineCount > 0) {
                translateY += between
            }

            pendingLines.add(StringWithPosition(x, y, lineWidth, y - translateY, line))
        }

        if (pendingLines.stream().allMatch { l: StringWithPosition ->
                l.text.isEmpty()
            }) {
            return emptyList()
        }

        return pendingLines
    }
}