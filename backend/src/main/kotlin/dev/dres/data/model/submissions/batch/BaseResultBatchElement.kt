package dev.dres.data.model.submissions.batch

import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.StatusAspect

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
interface BaseResultBatchElement : ItemAspect, StatusAspect
