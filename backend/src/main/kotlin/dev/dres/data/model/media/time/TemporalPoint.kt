package dev.dres.data.model.media.time

import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.PlayableMediaItem
import java.lang.IllegalArgumentException

/**
 * Notion of a [TemporalPoint] within a [DbMediaItem] that exhibits temporal development (e.g. [VideoItem]).
 *
 * @version 2.2.0
 * @author Luca Rossetto & Ralph Gasser
 */
sealed class TemporalPoint {
    abstract fun niceText(): String
    abstract fun toMilliseconds(): Long

    /**
     * A [TemporalPoint] represented by a frame number and a fps value.
     */
    data class Frame(val frame: Int, val fps: Float) : TemporalPoint(){

        companion object {
            fun toMilliseconds(frame: Int, framesPerSecond: Float) : Long = (frame / framesPerSecond * 1000f).toLong()
        }

        override fun niceText(): String = "FrameTemporalPoint(Frame $frame @ $fps fps)"

        override fun toMilliseconds(): Long = toMilliseconds(frame, fps)
    }

    /**
     * A [TemporalPoint] represented by a millsecond value.
     */
    data class Millisecond(val millisecond: Long) : TemporalPoint() {
        override fun niceText(): String = "MilliSecondTemporalPoint($millisecond)"

        override fun toMilliseconds(): Long = millisecond
    }

    /**
     * A [TemporalPoint] represented by a timecode and a fps value.
     */
    data class Timecode(val timecode: String, val fps: Float) : TemporalPoint(){

        companion object {
            private val timecodeRegex = "^\\s*(?:(?:(?:(\\d+):)?([0-5]?\\d):)?([0-5]?\\d):)?(\\d+)\\s*\$".toRegex()

            private const val msPerHour: Long = 3_600_000
            private const val msPerMinute: Long = 60_000

            /**
             * Transforms a time code of the form HH:MM:SS:FF to milliseconds
             * @return time in milliseconds or null if the input is not a valid time code
             */
            fun timeCodeToMilliseconds(timecode: String, fps: Float = 24.0f): Long? {

                val matches = timecodeRegex.matchEntire(timecode) ?: return null

                val hours = matches.groups[1]?.value?.toLong() ?: 0
                val minutes = matches.groups[2]?.value?.toLong() ?: 0
                val seconds = matches.groups[3]?.value?.toLong() ?: 0
                val frames = matches.groups[4]?.value?.toLong() ?: 0

                return hours * msPerHour + minutes * msPerMinute + seconds * 1000 + (1000 * frames / fps).toLong()
            }

            fun timeCodeToMilliseconds(timecode: String, item: PlayableMediaItem): Long? = timeCodeToMilliseconds(timecode, item.fps)
        }

        private val millisecond: Long = timeCodeToMilliseconds(timecode, fps) ?: throw IllegalArgumentException("'$timecode' is not a valid time code of the form HH:MM:SS:FF")

        override fun niceText(): String = "TimeCodeTemporalPoint($timecode with $fps fps)"

        override fun toMilliseconds(): Long = millisecond

    }
}

