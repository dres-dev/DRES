package dev.dres.run.score.interfaces

interface TaskRunScorer {

    /**
     * Returns the current scores for all teams in the relevant Task
     */
    fun scores(): Map<Int, Double>

}