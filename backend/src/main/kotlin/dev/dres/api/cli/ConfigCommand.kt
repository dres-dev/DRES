package dev.dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import dev.dres.DRES

class ConfigCommand:CliktCommand(name="config", help="Command that prints the current config in use") {

    override fun run() {
        println("Currently active configuration: ")
        println(" Application root: ${DRES.APPLICATION_ROOT}")
        println(" Data root: ${DRES.DATA_ROOT}")
        println(" External root: ${DRES.EXTERNAL_ROOT}")
        println(" HTTP Port: ${DRES.CONFIG.httpPort}")
        println(" HTTPS Port: ${DRES.CONFIG.httpsPort}")
        println(" Enable SSL: ${DRES.CONFIG.enableSsl}")
        println(" Cache information")
        println("  Cleanup interval (ms): ${DRES.CONFIG.cache.cleanupIntervalMs}")
        println("  Eviction threshold (ms): ${DRES.CONFIG.cache.evictionThresholdMs}")
        println("  FFMPEG: ${DRES.CONFIG.cache.ffmpegPath()}")
        println("  Logo max size: ${DRES.CONFIG.cache.logoMaxSize}")
        println("  Max rendering threads: ${DRES.CONFIG.cache.maxRenderingThreads}")
        println("  Preview image max size: ${DRES.CONFIG.cache.previewImageMaxSize}")
        println("  Preview video max size: ${DRES.CONFIG.cache.previewVideoMaxSize}")
    }
}
