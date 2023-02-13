package dev.dres.data.model.template.task

import dev.dres.data.model.template.TemplateId

interface TaskTemplate {
    fun textualDescription(): String

    val templateId: TemplateId

}