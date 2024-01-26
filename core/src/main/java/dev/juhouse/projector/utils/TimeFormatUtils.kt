package dev.juhouse.projector.utils

import java.util.Collections

class TimeFormatUtils {
    companion object {
        fun formatTimeStringToMs(time: String): Long {
            val textSplit = time.split(':').reversed()

            val secs = textSplit[0].ifEmpty { "0" }.toInt()
            val mins = textSplit.getOrNull(1)?.ifEmpty { "0" }?.toInt() ?: 0
            val hours = textSplit.getOrNull(2)?.ifEmpty { "0" }?.toInt() ?: 0

            return secs * 1000L + mins * 60 * 1000L + hours * 60 * 60 * 1000L
        }

        fun formatMsToTime(ms: Long): String {
            val secs = (ms % (60 * 1000)) / 1000
            val minutes = (ms % (60 * 60 * 1000)) / (60 * 1000)
            val hours = ms / (60 * 60 * 1000)

            return formatNumbersToTime(hours, minutes, secs)
        }

        fun formatNumbersToTime(hours: Int, minutes: Int, secs: Int): String {
            return formatNumbersToTime(hours.toLong(), minutes.toLong(), secs.toLong())
        }

        private fun formatNumbersToTime(hours: Long, minutes: Long, secs: Long): String {
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
    }
}