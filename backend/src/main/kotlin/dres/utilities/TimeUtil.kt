package dres.utilities

import dres.data.model.basics.time.TemporalPoint
import dres.data.model.basics.time.TemporalRange
import dres.data.model.basics.time.TemporalUnit

object TimeUtil {

    fun toMilliseconds(point: TemporalPoint, fps: Float = 24.0f): Long {
        return when (point.unit) {
            TemporalUnit.FRAME_NUMBER -> (point.value / fps * 1000).toLong()
            TemporalUnit.SECONDS -> (point.value * 1000).toLong()
            TemporalUnit.MILLISECONDS -> point.value.toLong()
        }
    }

    fun toMilliseconds(range: TemporalRange, fps: Float = 24.0f): Pair<Long, Long> = (
            toMilliseconds(range.start, fps) to toMilliseconds(range.end, fps)
            )

    /**
     * merges overlapping ranges
     */
    fun merge(ranges: List<TemporalRange>, fps: Float = 24.0f): List<TemporalRange> {

        if (ranges.isEmpty()){
            return emptyList()
        }

        val pairs = ranges.map { toMilliseconds(it, fps) }.sortedBy { it.first }

        var i = 1
        var current = pairs.first()

        val merged = mutableListOf<Pair<Long, Long>>()

        while (i < pairs.size){
            val next = pairs[i]

            //if overlapping, merge
            current = if (current.second >= next.first){
                current.copy(second = next.second)
            } else { //else add to list and continue
                merged.add(current)
                next
            }
            ++i
        }

        return merged.map { TemporalRange(it.first, it.second) }

    }

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
}