package dev.dres.data.model.competition.options

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class TargetOption : Option {
    SINGLE_MEDIA_ITEM, // Whole Media Item"
    SINGLE_MEDIA_SEGMENT, //Part of a Media Item
    MULTIPLE_MEDIA_ITEMS, //Multiple Media Items
    JUDGEMENT, //Judgement
    VOTE //Judgement with audience voting
}