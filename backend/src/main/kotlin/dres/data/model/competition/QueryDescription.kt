package dres.data.model.competition

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.Serializable

@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = QueryDescription.VideoQueryDescription::class, name = "VIDEO"),
        JsonSubTypes.Type(value = QueryDescription.ImageQueryDescription::class, name = "IMAGE"),
        JsonSubTypes.Type(value = QueryDescription.TextQueryDescription::class, name = "TEXT")
)
sealed class QueryDescription(val type: QueryDescriptionType) {
    /**
     * A query description that consists of video data encoded in Base 64 format.
     *
     * @author Ralph Gasser
     * @version 1.0
     */
    class VideoQueryDescription(val taskName: String, val video: String, val contentType: String) : QueryDescription(QueryDescriptionType.VIDEO)

    /**
     * A query description that consists of image data encoded in Base 64 format.
     *
     * @author Ralph Gasser
     * @version 1.0
     */
    class ImageQueryDescription(val taskName: String, val image: String, val contentType: String) : QueryDescription(QueryDescriptionType.IMAGE)

    /**
     * A query description that consists of a list of textual hints.
     *
     * @author Ralph Gasser
     * @version 1.0
     */
    class TextQueryDescription(val taskName: String, val text: List<TextualDescription>) : QueryDescription(QueryDescriptionType.TEXT) {
        class TextualDescription(val showAfter: Int, val text: String)
    }
}


