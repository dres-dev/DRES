package dres.data.model.competition

import kotlinx.serialization.Serializable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.xml.bind.DatatypeConverter

@Serializable
data class Team (val name: String, val color: String, val logo: String) {
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