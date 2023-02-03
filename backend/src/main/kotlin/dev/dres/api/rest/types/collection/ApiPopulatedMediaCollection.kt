package dev.dres.api.rest.types.collection

/**
 * A [ApiMediaCollection] populated with its items. No pagination, populated with all items.
 */
data class ApiPopulatedMediaCollection (val collection:ApiMediaCollection, val items: List<ApiMediaItem>)
