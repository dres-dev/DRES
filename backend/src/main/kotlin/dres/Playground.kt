package dres

import dres.utilities.FFmpegUtil
import java.nio.file.Path


object Playground {

    @JvmStatic
    fun main(args: Array<String>) {


        FFmpegUtil.extractSegment(
                Path.of("v3c1/testVideo.mp4"),
                "1",
                "3",
                Path.of("out.mp4")
        )


    }

}