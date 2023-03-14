package dev.juhouse.projector.projection2.text

import java.awt.FontMetrics

data class TextWrapperMetricsFitInLine(
    val renderId: Int,
    val lines: List<String>,
    val lineLength: Int
) {
    fun stripAt(position: Int): TextWrapperMetricsFitInLine {
        val result = ArrayList<String>()
        var pos = 0

        lines.forEach {
            if (pos + it.length < position) {
                result.add(it)
            } else if (pos <= position) {
                result.add(it.substring(0, position - pos))
            }

            pos += it.length
        }

        return TextWrapperMetricsFitInLine(renderId, lines, position)
    }
}

data class TextWrapperMetrics(
    val renderId: Int,
    val fontMetrics: FontMetrics,
    val textAreaWidth: Int,
    val textAreaHeight: Int
) {
    // This may occur with gigant font sizes.
    // TODO: Fix this workaround
    // Beware! zero lines may bugs lot of things!
    private val maxLines: Int get() = (textAreaHeight / (fontMetrics.height + fontMetrics.leading + fontMetrics.descent)).coerceAtLeast(1)

    private fun isSeparator(c: Char): Boolean {
        return Character.isWhitespace(c) || c == ','
    }

    /**
     * Returns the index of the first whitespace character or '-' in
     * <var>line</var>
     * that is at or before <var>start</var>. Returns -1 if no such character is
     * found.
     *
     * @param line a string
     * @param start where to star looking
     */
    private fun findBreakBefore(line: String, start: Int): Int {
        for (i in start downTo 0) {
            val c = line[i]
            if (isSeparator(c)) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the index of the first whitespace character or '-' in
     * <var>line</var>
     * that is at or after <var>start</var>. Returns -1 if no such character is
     * found.
     *
     * @param line a string
     * @param start where to star looking
     */
    private fun findBreakAfter(line: String, start: Int): Int {
        val len = line.length
        for (i in start until len) {
            val c = line[i]
            if (isSeparator(c)) {
                return i
            }
        }
        return -1
    }

    fun getFitInLines(line: String): TextWrapperMetricsFitInLine {
        val result = ArrayList<String>()
        var currentLine = line
        var currentPosition = 0

        do {
            do {
                val len = currentLine.length
                val width = fontMetrics.stringWidth(currentLine)

                if (width <= textAreaWidth || len <= 0) {
                    break;
                }

                val guess = len * textAreaWidth / width;
                val before = line.substring(0, guess).trim()

                val guessWidth = fontMetrics.stringWidth(before);

                if (guessWidth <= textAreaWidth) {
                    val pos = findBreakAfter(currentLine, guess)

                    if (pos >= 0) {
                        val newLine = currentLine.substring(0, pos).trim()

                        if (fontMetrics.stringWidth(newLine) <= textAreaWidth) {
                            currentLine = newLine
                            break;
                        }
                    }
                }

                currentLine = currentLine.substring(0, findBreakBefore(currentLine, guess)).trim()
            } while (true)

            result.add(currentLine)
            currentPosition += currentLine.length

            currentLine = line.substring(currentPosition)
        } while (currentLine.isNotEmpty() && result.size <= maxLines)

        return TextWrapperMetricsFitInLine(renderId, result, currentPosition)
    }
}
