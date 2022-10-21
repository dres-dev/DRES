package dev.dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.kittinunf.fuel.Fuel

import java.io.File

class OpenApiCommand : CliktCommand(name="openapi",help =  "Generates and writes the OpenAPI Specifications") {

    /** Defaults to only write internal OpenApi Specification (OAS) */
    val internalOnly: Boolean by option("--dres", "--internal-only").flag("--all", default=true)

    val verbose: Boolean by option("-v", "--verbose").flag("--quiet", "-q", default=false)

    val output: File by option("-o", "--out").file().default(File("build/api"))

    override fun run() {
        // Do we download the client lib as well?
        if(!internalOnly){
            downloadOas("swagger-docs", output)
        }
        downloadOas("client-oas", output)
    }

    private fun downloadOas(path: String, dir: File){
        val src = "http://localhost:8080/$path"
        vprintln("Downloading from $src")
        val fileName = "${path}.json"
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