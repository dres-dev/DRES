package dres.api.rest.types.run

import dres.data.model.run.CompetitionRun
import dres.run.RunManager

/**
 * Basic information regarding a viewer instance
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class ViewerInfo(val viewersId: String, val ready: Boolean)