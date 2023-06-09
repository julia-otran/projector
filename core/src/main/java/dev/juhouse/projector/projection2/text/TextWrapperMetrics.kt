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
                val str = it.substring(0, position - pos).trim()

                if (str.isNotEmpty()) {
                    result.add(str)
                }
            }

            pos += it.length + 1
        }

        return TextWrapperMetricsFitInLine(renderId, result, position)
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
        for (i in start - 1 downTo 0) {
            val c = line[i]
            if (isSeparator(c)) {
                return i + 1
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
                return i + 1
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

                val guess = len * textAreaWidth / width
                val before = currentLine.substring(0, guess).trim()

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

                val beforeBreak = findBreakBefore(currentLine, guess)

                if (beforeBreak < 0) {
                    break
                }

                val beforeBreakString = currentLine.substring(0, beforeBreak).trim()

                if (beforeBreakString == currentLine) {
                    break
                }

                currentLine = beforeBreakString
            } while (true)

            result.add(currentLine)
            currentPosition += currentLine.length

            while (currentPosition < line.length && Character.isWhitespace(line[currentPosition])) {
                currentPosition += 1;
            }

            currentLine = line.substring(currentPosition).trim()
        } while (currentLine.isNotEmpty() && result.size <= maxLines)

        return TextWrapperMetricsFitInLine(renderId, result, currentPosition)
    }
}
