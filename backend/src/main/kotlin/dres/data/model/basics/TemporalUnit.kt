package dres.data.model.basics

/**
 * Time units used in [TemporalPoint]s and [TemporalRange]s
 *
 * @author Ralph Gasser
 * @version 1.0
 *
 * TODO: Conversion (if possible).
 */
enum class TemporalUnit {
    FRAME_NUMBER,
    SECONDS,
    MILLISECONDS,
    TIMECODE
}