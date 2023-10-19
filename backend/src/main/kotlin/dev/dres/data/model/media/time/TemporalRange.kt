package dev.dres.data.model.media.time

/**
 * Notion of a [TemporalRange] within a [MediaItem] that exhibits temporal development (e.g. [VideoItem].
 *
 * @version 2.1.0
 *
 * @param start The start of the [TemporalRange]
 * @param end The end of the [TemporalRange]
 */
data class TemporalRange constructor(val start: TemporalPoint, val end: TemporalPoint) {

    constructor(startMs: Long, endMs: Long) : this(TemporalPoint.Millisecond(startMs), TemporalPoint.Millisecond(endMs))
    constructor(pair: Pair<Long, Long>) : this(pair.first, pair.second)

    init {
        require(start.toMilliseconds() <= end.toMilliseconds()) {"Start point must be before End point in TemporalRange"}
    }

    companion object {
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
    }


    /**
     * Returns the duration of this [TemporalRange] in milliseconds.
     */
    fun durationMs(): Long = this.end.toMilliseconds() - this.start.toMilliseconds()

    fun contains(inner: TemporalRange): Boolean =
            start.toMilliseconds() <= inner.start.toMilliseconds() &&
                    end.toMilliseconds() >= inner.end.toMilliseconds()

    fun overlaps(other: TemporalRange) : Boolean {
        val s1 = start.toMilliseconds()
        val s2 = other.start.toMilliseconds()
        val e1 = end.toMilliseconds()
        val e2 = other.end.toMilliseconds()

        return s1 <= e2 && s2 <= e1
    }

    fun toMilliseconds(): Pair<Long, Long> = Pair(start.toMilliseconds(), end.toMilliseconds())

    val center
        get() = (end.toMilliseconds() - start.toMilliseconds()) / 2
}
