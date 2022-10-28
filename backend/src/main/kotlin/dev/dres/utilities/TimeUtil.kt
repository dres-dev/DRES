package dev.dres.utilities

import dev.dres.data.model.media.MediaItemSegmentList
import dev.dres.data.model.media.time.TemporalRange
import kotlin.math.abs

object TimeUtil {

    /**
     * merges overlapping ranges
     */
    fun merge(ranges: List<TemporalRange>, overlap: Int = 0): List<TemporalRange> {

        if (ranges.isEmpty()){
            return emptyList()
        }

        val pairs = ranges.map { it.toMilliseconds() }.sortedBy { it.first }

        var i = 1
        var current = pairs.first()

        val merged = mutableListOf<Pair<Long, Long>>()

        while (i < pairs.size){
            val next = pairs[i]

            //if overlapping, merge
            current = if (current.second + overlap >= next.first){
                current.copy(second = next.second)
            } else { //else add to list and continue
                merged.add(current)
                next
            }
            ++i
        }
        merged.add(current)

        return merged.map { TemporalRange(it.first, it.second) }

    }

    /**
     * Converts a shot number to a timestamp in milliseconds.
     */
    fun shotToTime(shot: String, segmentList: MediaItemSegmentList): Pair<Long,Long>? {
        val segment = segmentList.segments.find { it.name == shot } ?: return null
        return segment.range.toMilliseconds()
    }


    fun timeToSegment(time: Long, segmentList: MediaItemSegmentList): Pair<Long,Long>? {
        if (segmentList.segments.isEmpty()) {
            return null
        }
        val segment = segmentList.segments.find {
            val range = it.range.toMilliseconds()
            range.first <= time && range.second >= time
        } ?: segmentList.segments.minByOrNull { abs(it.range.center - time) }!!

        return segment.range.toMilliseconds()
    }


}