package dev.juhouse.projector.utils

class TimeFormatUtils {
    companion object {
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