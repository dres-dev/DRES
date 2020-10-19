package dev.dres.api.rest.types.competition

import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.competition.Team
import dev.dres.utilities.extensions.UID
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.xml.bind.DatatypeConverter


data class RestTeam(val name: String,
                    val color: String,
                    val logoData: String?,
                    val logoId: String?,
                    val users: List<String>) {

    constructor(team: Team) : this(
            name = team.name,
            color = team.color,
            logoData = null,
            logoId = team.logoId.string,
            users = team.users.map { it.string }
    )


    companion object {
        /**
         * Stores the given image data to disk.
         *
         * @param config The [Config] object with global configuration.
         * @param data The Base64 encoded image data.
         * @return The UID of the image.
         */
        fun storeImage(config: Config, data: String): UID {
            /* Parse image data. */
            val base64Image: String = data.substringAfter(",")
            val imageBytes = DatatypeConverter.parseBase64Binary(base64Image)
            val image = ByteArrayInputStream(imageBytes).use {
                val original = ImageIO.read(it)
                if (original.width <= config.logoMaxSize && original.height <= config.logoMaxSize) {
                    original
                } else {
                    val target = if (original.width > original.height) {
                        Pair(config.logoMaxSize, (original.height * (config.logoMaxSize.toDouble() / original.width)).toInt())
                    } else {
                        Pair((original.width * (config.logoMaxSize.toDouble() / original.height)).toInt(), config.logoMaxSize)
                    }
                    val resizedImage = BufferedImage(target.first, target.second, BufferedImage.TYPE_INT_RGB)
                    val graphics2D = resizedImage.createGraphics()
                    graphics2D.drawImage(original, 0, 0, target.first, target.second, null)
                    graphics2D.dispose()
                    resizedImage
                }
            }

            /* Generate UID and prepare file path. */
            val uid = UID()
            val path = Team.logoPath(config, uid)
            if (!Files.exists(path.parent)) {
                Files.createDirectories(path.parent)
            }

            /* Generate UID and write image to disk. */
            Files.newOutputStream(path).use {
                ImageIO.write(image, "PNG", it)
            }
            return uid
        }
    }

    /**
     * Converts this [RestTeam] to a [Team] object.
     *
     * @param config The [Config] object with global configuration.
     * @return [Team]
     */
    fun toTeam(config: Config): Team = when {
        this.logoId != null -> Team(this.name, this.color, this.logoId, this.users.map { it.UID() }.toMutableList())
        this.logoData != null -> Team(this.name, this.color, storeImage(config, this.logoData).string, this.users.map { it.UID() }.toMutableList())
        else -> throw IllegalStateException("Cannot convert to Team, because it contains no logo information.")
    }
}