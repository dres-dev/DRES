package dev.dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.kittinunf.fuel.Fuel
import dev.dres.api.rest.OpenApiEndpointOptions
import dev.dres.api.rest.RestApi
import io.swagger.util.Json
import java.io.File

class OpenApiCommand : CliktCommand(name="openapi",help =  "Generates and writes the OpenAPI Specifications") {

    /** Defaults to only write internal OpenApi Specification (OAS) */
    val internalOnly: Boolean by option("--dres", "--internal-only").flag("--all", default=true)

    val verbose: Boolean by option("-v", "--verbose").flag("--quiet", "-q", default=false)

    val output: File by option("-o", "--out").file().default(File("build/api"))

    override fun run() {
        // Do we download the client lib as well?
        if(!internalOnly){
            downloadOas(OpenApiEndpointOptions.dresSubmittingClientOptions, output)
        }
        downloadOas(OpenApiEndpointOptions.dresDefaultOptions, output)
    }

    private fun downloadOas(oas: OpenApiEndpointOptions, dir: File){
        val src = "http://localhost:8080"+oas.oasPath
        vprintln("Downloading from $src")
        val fileName = oas.oasPath.substring(1)+".json" // Remove "/" from name and add ".json"
        Fuel.download(src)
            .fileDestination { _, _ -> dir.resolve(fileName) }
            .progress{readBytes, totalBytes ->
                val progress = readBytes.toFloat() / totalBytes.toFloat() * 100
                vprintln("Bytes downloaded $readBytes / $totalBytes ($progress %)")
            }
        vprintln("Finished from $src")
    }

    private fun vprintln(str:String){
        if(verbose){
            println(str)
        }
    }

}