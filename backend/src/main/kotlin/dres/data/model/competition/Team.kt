package dres.data.model.competition


import dres.api.rest.types.competition.RestTeam
import dres.data.model.UID
import dres.utilities.extensions.UID
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.xml.bind.DatatypeConverter


data class Team constructor(
        val name: String,
        val color: String,
        val logo: String,
        val users: MutableList<UID>) {

    constructor(restTeam: RestTeam) : this(
        restTeam.name,
        restTeam.color,
        restTeam.logo,
        restTeam.users.map { it.UID() }.toMutableList()
    )

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