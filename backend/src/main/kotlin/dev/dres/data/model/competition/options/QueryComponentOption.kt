package dev.dres.data.model.competition.options

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class QueryComponentOption: Option {
    IMAGE_ITEM, //Image Media Item
    VIDEO_ITEM_SEGMENT, //Part of a Video Media Item
    TEXT,
    EXTERNAL_IMAGE,
    EXTERNAL_VIDEO
}