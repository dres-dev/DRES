package dev.dres.api.rest.types.competition.team

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.kittinunf.fuel.util.decodeBase64
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.Team
import dev.dres.data.model.template.team.TeamId
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * A RESTful API representation of a [DbTeam]
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class ApiTeam(
    val id: TeamId? = null,
    val name: String? = null,
    val color: String? = null,
    val users: List<ApiUser> = emptyList(),
    var logoData: String? = null
) : Team {
    override val teamId: TeamId
        @JsonIgnore(true)
        get() = this.id ?: "<unspecified>"


    fun logoStream() : ByteArrayInputStream? = this.logoData?.let { submitted -> ByteArrayInputStream(normalizeLogo(submitted)) }
    companion object {

        /**
         * Tries to normalize the logo data to a PNG image of a given size.
         *
         * @param submittedData The submitted data as base 64 encoded string.
         * @return [ByteArray] representation of the normalized log.
         */
        fun normalizeLogo(submittedData: String): ByteArray? {
            /* Try to read image. */
            val image: BufferedImage = submittedData.drop(submittedData.indexOf(',') + 1).decodeBase64().let {
                try {
                    ImageIO.read(ByteArrayInputStream(it))
                }  catch (e: Exception) {
                    null
                }
            } ?: return null

            /* Scale image to a maximum of 500x500 pixels. */
            val scaled: Image = if (image.width > image.height) {
                image.getScaledInstance(500, -1, Image.SCALE_DEFAULT)
            } else {
                image.getScaledInstance(-1, 500, Image.SCALE_DEFAULT)
            }
            val outputImage = BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_ARGB)
            outputImage.graphics.drawImage(scaled, 0, 0, null);

            /* Write image as PNG. */
            val out = ByteArrayOutputStream()
            ImageIO.write(outputImage, "png", out)
            return out.toByteArray()
        }

    }

}
