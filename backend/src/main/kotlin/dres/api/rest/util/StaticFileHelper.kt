package dres.api.rest.util

import io.javalin.http.Context
import java.io.File

object StaticFileHelper{

    fun serveFile(ctx: Context, file: File) {
        if (!file.exists()){
            ctx.status(404)
            return
        }

        ctx.status(200)
        ctx.contentType("image/png") //TODO figure out mime type
        file.inputStream().copyTo(ctx.res.outputStream)

    }

}