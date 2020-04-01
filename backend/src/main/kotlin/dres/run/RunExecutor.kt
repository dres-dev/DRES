package dres.run

import io.javalin.Javalin
import io.javalin.websocket.WsHandler
import java.util.function.Consumer

object RunExecutor {




    
    fun listRuns(): List<RunManager> {
        return emptyList()
    }

    /**
     *
     */
    fun schedule(manager: RunManager) {
        Javalin.create().ws("/competition/run/1") {
            it.onConnect {

            }
        }
    }

}