package dev.dres.api.rest.types.competition

import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.competition.Team
import dev.dres.utilities.extensions.UID
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.*
import javax.imageio.ImageIO


data class RestTeam(val uid: String? = null,
                    val name: String,
                    val color: String,
                    val logoData: String?,
                    val logoId: String?,
                    val users: List<String>) {

    constructor(team: Team) : this(
        uid = team.uid.string,
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
         * @param logoId The [UID] of the logo to store.
         *
         * @return The UID of the image.
         */
        fun storeLogo(config: Config, data: String, logoId: UID = UID()): UID {
            /* Parse image data. */
            val base64Image: String = data.substringAfter(",")
            val imageBytes = Base64.getDecoder().decode(base64Image)
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
                    val resizedImage = BufferedImage(target.first, target.second, BufferedImage.TYPE_INT_ARGB)
                    val graphics2D = resizedImage.createGraphics()
                    graphics2D.drawImage(original, 0, 0, target.first, target.second, null)
                    graphics2D.dispose()
                    resizedImage
                }
            }

            /* Generate UID and prepare file path. */
            val path = Team.logoPath(config, logoId)
            if (!Files.exists(path.parent)) {
                Files.createDirectories(path.parent)
            }

            /* Generate UID and write image to disk. */
            Files.newOutputStream(path).use {
                ImageIO.write(image, "PNG", it)
            }
            return logoId
        }
    }

    /**
     * Converts this [RestTeam] to a [Team] object.
     *
     * @param config The [Config] object with global configuration.
     * @return [Team]
     */
    fun toTeam(config: Config): Team {
        val logoId = this.logoId?.UID() ?: UID()
        if (this.logoData != null) {
            storeLogo(config, this.logoData, logoId)
        }
        return Team(this.uid?.UID() ?: UID(), this.name, this.color, logoId, this.users.map { it.UID() }.toMutableList())
    }
}