package dev.dres.api.rest.handler.preview

import dev.dres.DRES
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.media.DbMediaItem
import dev.dres.mgmt.cache.CacheManager
import dev.dres.utilities.extensions.errorResponse
import dev.dres.utilities.extensions.sessionToken
import dev.dres.utilities.extensions.streamFile
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Serves the single media payload designated by a [MediaGrant].
 *
 * This replaces embedding base64 media in hint and target responses. The URL is unguessable, bound to
 * the session that requested the hint or target, expires after [MediaGrantRegistry.DEFAULT_TTL_MS],
 * and permits a bounded number of redemptions, so a copied URL is of little use to anyone else.
 *
 * Redemption is intentionally cheap: for the common case the payload was rendered while the hint or
 * target was built ([MediaGrantPayload.Preview]), so this handler only performs a lookup and a file
 * stream. That is what keeps the viewer's waiting time low without letting the browser fetch payloads
 * it was never granted.
 *
 * @author DRES
 * @version 1.0.0
 */
class GetMediaGrantHandler(private val store: TransientEntityStore, private val cache: CacheManager) :
    GetRestHandler<Any>, AccessManagedRestHandler {

    override val route: String = "media/grant/{grantId}"

    /**
     * Same audience as direct media retrieval; the grant itself carries the restriction, and
     * [MediaGrantRegistry.redeem] rejects a session the grant was not minted for.
     */
    override val permittedRoles = setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE, ApiRole.ADMIN)

    override val apiVersion = RestApi.LATEST_API_VERSION

    //not used
    override fun doGet(ctx: Context): Any = ""

    @OpenApi(
        summary = "Returns the media payload designated by a short-lived grant.",
        path = "/api/v2/media/grant/{grantId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("grantId", String::class, "The opaque grant identifier.", required = true)
        ],
        tags = ["Media"],
        responses = [
            OpenApiResponse("200"),
            OpenApiResponse("401"),
            OpenApiResponse("403"),
            OpenApiResponse("404"),
            OpenApiResponse("410")
        ],
        methods = [HttpMethod.GET]
    )
    /**
     * Serves the granted payload.
     *
     * Overridden instead of using the default [GetRestHandler.get] body because that body would hand the
     * result to `ctx.json(...)`; this handler writes the file itself. [ErrorStatusException]s are therefore
     * translated here, exactly as the default body does for handlers that return a value.
     */
    override fun get(ctx: Context) {
        try {
            val grantId = ctx.pathParamMap()["grantId"]
                ?: throw ErrorStatusException(400, "Parameter 'grantId' not specified.", ctx)
            val session = ctx.sessionToken()
                ?: throw ErrorStatusException(401, "Media grants can only be redeemed with a session.", ctx)

            /* Meter before redeeming, so a hostile client cannot spend a grant's budget faster than the limit. */
            MediaRequestGuard.enforce(ctx, "media-grant", grantId)

            val grant = MediaGrantRegistry.redeem(grantId, session)
                ?: throw ErrorStatusException(410, "Media grant is invalid, expired, or does not belong to this session.", ctx)

            val path = this.resolve(grant.payload, ctx)

            /* The capability is scoped to one session, so shared caches may keep the response for the
             * grant's remaining lifetime; `private` keeps one session's cached bytes away from another
             * session of the same deployment, and range requests must keep working for seeking. */
            ctx.header("Cache-Control", "private, max-age=${grant.remainingSeconds()}")
            try {
                ctx.streamFile(path)
            } catch (e: org.eclipse.jetty.io.EofException) {
                /* Triggered by a client that stops playback; safe to ignore. */
            }
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        }
    }

    /**
     * Resolves the file a payload designates, rendering it if it is not prepared yet.
     *
     * @param payload The [MediaGrantPayload] to resolve.
     * @param ctx The request [Context], used for error reporting.
     * @return The [Path] to serve.
     * @throws ErrorStatusException If the payload no longer exists or cannot be produced.
     */
    private fun resolve(payload: MediaGrantPayload, ctx: Context): Path {
        val path = when (payload) {
            is MediaGrantPayload.Preview -> payload.path
            is MediaGrantPayload.Item -> this.store.transactional(true) {
                val item = this@GetMediaGrantHandler.item(payload.itemId, ctx)
                item.pathToOriginal()
            }
            is MediaGrantPayload.ItemSegment -> this.store.transactional(true) {
                val item = this@GetMediaGrantHandler.item(payload.itemId, ctx)
                this@GetMediaGrantHandler.cache.asyncPreviewVideo(item, payload.start, payload.end).get()
            }
            is MediaGrantPayload.ExternalFile ->
                ExternalMediaPath.resolve(DRES.EXTERNAL_ROOT, payload.file)
                    ?: throw ErrorStatusException(404, "Granted external media no longer exists.", ctx)
            is MediaGrantPayload.ExternalSegment -> {
                val source = ExternalMediaPath.resolve(DRES.EXTERNAL_ROOT, payload.file)
                    ?: throw ErrorStatusException(404, "Granted external media no longer exists.", ctx)
                this.cache.asyncPreviewVideo(source, payload.start, payload.end).get()
            }
        }
        if (!Files.exists(path)) throw ErrorStatusException(404, "Granted media could not be found.", ctx)
        return path
    }

    /** Loads a [DbMediaItem] or fails with 404. Requires an active transaction. */
    private fun item(itemId: String, ctx: Context): DbMediaItem =
        DbMediaItem.query(DbMediaItem::id eq itemId).firstOrNull()
            ?: throw ErrorStatusException(404, "Granted media item no longer exists.", ctx)

    companion object {

        /**
         * Builds the public URL of a grant relative to the API root, e.g. `media/grant/{id}`.
         *
         * Clients resolve it against their own base URL, so the value is stable across host changes
         * (reverse proxies, tunnels) in a way that an absolute URL would not be.
         *
         * @param grant The [MediaGrant].
         * @return The URL path of the grant.
         */
        fun grantPath(grant: MediaGrant): String = "media/grant/${grant.id}"
    }
}
