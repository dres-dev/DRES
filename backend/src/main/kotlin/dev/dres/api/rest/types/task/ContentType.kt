package dev.dres.api.rest.types.task

enum class ContentType(val mimeType: String, val base64: Boolean) {
    EMPTY("", false),
    TEXT("text/plain", false),
    VIDEO("video/mp4", true),
    IMAGE("image/jpg", true)
}