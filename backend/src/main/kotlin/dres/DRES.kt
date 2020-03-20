package dres

import dres.api.rest.RestApi
import dres.data.Config
import java.io.File

object DRES {

    @JvmStatic
    fun main(args: Array<String>) {

        val config = if (args.isNotEmpty()) {
            Config.read(File(args[0]))
        } else {
            null
        } ?: Config()

        RestApi.init(config)

    }

}
