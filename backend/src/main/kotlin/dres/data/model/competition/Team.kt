package dres.data.model.competition

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.xml.bind.DatatypeConverter


data class Team @JsonCreator constructor(
        @JsonProperty("name") val name: String,
        @JsonProperty("color")  val color: String,
        @JsonProperty("logo")  val logo: String,
        @JsonProperty("users")  val users: MutableList<Long>) {
    /**
     * Returns the logo data as [BufferedImage].
     *
     * @return [BufferedImage] of the logo.
     */
    fun logo(): BufferedImage {
        val base64Image: String = this.logo.split(",")[1]
        val imageBytes = DatatypeConverter.parseBase64Binary(base64Image)
        ByteArrayInputStream(imageBytes).use {
            return ImageIO.read(it)
        }
    }
}