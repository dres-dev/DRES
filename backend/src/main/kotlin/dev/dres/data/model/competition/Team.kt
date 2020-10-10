package dev.dres.data.model.competition


import dev.dres.api.rest.types.competition.RestTeam
import dev.dres.data.model.UID
import dev.dres.utilities.extensions.UID
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
        val imageBytes = logoData().second
        ByteArrayInputStream(imageBytes).use {
            return ImageIO.read(it)
        }
    }

    fun logoData(): Pair<String, ByteArray> {

        val base64Image: String = this.logo.substringAfter(",")
        val imageBytes = DatatypeConverter.parseBase64Binary(base64Image)

        val mimeType = this.logo.substringBefore(";").substringBefore(":")

        return Pair(mimeType, imageBytes)

    }
}