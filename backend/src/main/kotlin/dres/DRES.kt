package dres

import dres.api.cli.Cli
import dres.api.rest.RestApi
import dres.data.dbo.DataAccessLayer
import dres.data.model.Config
import java.io.File
import java.nio.file.Paths

object DRES {

    @JvmStatic
    fun main(args: Array<String>) {

        val config = if (args.isNotEmpty()) {
            Config.read(File(args[0]))
        } else {
            null
        } ?: Config()


        /* Initialize data access layer. */
        val dataAccessLayer = DataAccessLayer(Paths.get(config.dataPath))


        RestApi.init(config, dataAccessLayer)

        Cli.loop(dataAccessLayer) //blocks until quit command is given
        RestApi.stop()

    }
}
