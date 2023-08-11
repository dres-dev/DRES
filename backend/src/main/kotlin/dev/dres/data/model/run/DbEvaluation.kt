package dev.dres.data.model.run

import com.fasterxml.jackson.module.kotlin.*
import dev.dres.api.rest.types.evaluation.ApiEvaluation
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.run.interfaces.Evaluation
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.NonInteractiveRunManager
import dev.dres.run.RunManager
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.size

/**
 * Represents a [DbEvaluation], i.e., a concrete instance of a [DbEvaluationTemplate], as executed by DRES.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class DbEvaluation(entity: Entity) : PersistentEntity(entity), Evaluation {
    companion object : XdNaturalEntityType<DbEvaluation>()

    override fun constructor() {
        super.constructor()
        created = System.currentTimeMillis()
        status = DbEvaluationStatus.CREATED
    }

    /** The [EvaluationId] of this [DbEvaluation]. */
    override var evaluationId: EvaluationId
        get() = this.id
        set(value) {
            this.id = value
        }

    /** The name held by this [DbEvaluation]. Must be unique!*/
    var name by xdRequiredStringProp(trimmed = true)

    /** The [DbEvaluationType] of this [DbEvaluation]. */
    var type by xdLink1(DbEvaluationType)

    /** The [DbEvaluationStatus] of this [DbEvaluation]. */
    var status by xdLink1(DbEvaluationStatus)

    /** The [DbEvaluationTemplate] backing this [DbEvaluation]. */
    var template by xdLink1(DbEvaluationTemplate)

    /** Timestamp of when this [DbEvaluation] was created. */
    var created by xdRequiredLongProp()

    /** Timestamp of when this [DbEvaluation] started. */
    var started by xdNullableLongProp()

    /** Timestamp of when this [DbEvaluation] ended. */
    var ended by xdNullableLongProp()

    /** The [DbTask]s that belong to this [DbEvaluation]. */
    val tasks by xdChildren0_N<DbEvaluation, DbTask>(DbTask::evaluation)

    /** Flag indicating that participants can also use the viewer for this [DbEvaluation]. */
    var participantCanView by xdBooleanProp()

    /** Flag indicating that tasks should be shuffled. is only used for asynchronous runs */
    var shuffleTasks by xdBooleanProp()

    /** Flag indicating that tasks can be repeated. is only used for asynchronous runs */
    var allowRepeatedTasks by xdBooleanProp()

    /** A fixed limit on submission previews. */
    var limitSubmissionPreviews by xdIntProp()

    /** A serialized representation of the task permutation map used for [InteractiveAsynchronousEvaluation]s */
    var taskPermutationString by xdStringProp()

    fun permutation() : Map<TeamId, List<Int>>? {
        val mapper = jacksonObjectMapper()

        return if (taskPermutationString != null) {
            try {
                mapper.readValue<Map<TeamId, List<Int>>>(taskPermutationString!!)
            } catch (e: Exception) { //parsing failed for some reason TODO log
                null
            }
        } else {
            null
        }
    }

    fun initPermutation() {
        val mapper = jacksonObjectMapper()
        val permutation = generatePermutation()
        taskPermutationString = mapper.writeValueAsString(permutation)
    }

    private fun generatePermutation(): Map<TeamId, List<Int>> =
        if (shuffleTasks) {
            template.teams.asSequence().associate { it.id to makeLoop(template.tasks.size()) }
        } else {
            template.teams.asSequence().associate { it.id to template.tasks.asSequence().toList().indices.toList() }
        }

    /**
     * generates a sequence of tasks that loops through all tasks exactly once
     */
    private fun makeLoop(length: Int): List<Int> {
        if (length <= 0) {
            return emptyList()
        }
        val positions = (0 until length).shuffled()
        val array = IntArray(length) { -1 }

        fun recursionStep(open: List<Int>, idx: Int): Boolean {

            //nothing left to do
            if (open.isEmpty()) {
                return true
            }

            //invalid state, need to backtrack
            if (array[idx] != -1) {
                return false
            }

            //for all remaining options...
            for (nextPosition in open) {
                //...assign the next one...
                array[idx] = nextPosition
                //...and continue recursively
                if (recursionStep(
                        (open - nextPosition), //without the last assigned value
                        (nextPosition + 1) % array.size
                    ) //at the index after the last assigned position
                ) {
                    //assignment succeeded
                    return true
                }
            }

            //there was no valid assignment in the given options, need to back track
            array[idx] = -1
            return false
        }

        if (!recursionStep(positions, 0)) {
            error("Error during generation of task sequence")
        }

        return array.toList()
    }


    /**
     * Converts this [DbEvaluation] to a RESTful API representation [ApiEvaluation].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiEvaluation]
     */
    fun toApi(): ApiEvaluation = ApiEvaluation(
        evaluationId = this.evaluationId,
        name = this.name,
        type = this.type.toApi(),
        template = this.template.toApi(),
        created = this.created,
        started = this.started,
        ended = this.ended,
        tasks = this.tasks.asSequence().map { it.toApi() }.toList()
    )

    /**
     * Generates and returns an [RunManager] instance for this [DbEvaluation].
     *
     * @return [EvaluationRun]
     */
    fun toRunManager(store: TransientEntityStore): RunManager = when (this.type) {
        DbEvaluationType.INTERACTIVE_SYNCHRONOUS -> InteractiveSynchronousRunManager(InteractiveSynchronousEvaluation(store, this), store)
        DbEvaluationType.INTERACTIVE_ASYNCHRONOUS -> InteractiveAsynchronousRunManager(InteractiveAsynchronousEvaluation(store, this), store)
        DbEvaluationType.NON_INTERACTIVE -> NonInteractiveRunManager(NonInteractiveEvaluation(store, this), store)
        else -> throw IllegalArgumentException("Unsupported run type ${this.type.description}. This is a programmer's error!")
    }
}