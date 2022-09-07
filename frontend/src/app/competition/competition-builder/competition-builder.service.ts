import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {RestCompetitionDescription, RestTaskDescription, TaskGroup, TaskType} from '../../../../openapi';


/**
 * A service to share the currently editing competition among the competition builder components
 */
@Injectable({
    providedIn: 'root'
})
export class CompetitionBuilderService {

    private shouldLogDirtyChanges = true;
    private competitionSubject: BehaviorSubject<RestCompetitionDescription> = new BehaviorSubject<RestCompetitionDescription>(null);
    private dirty = false;

    constructor() {
    }

    public initialise(competition: RestCompetitionDescription) {
        this.competitionSubject.next(competition);
        this.unmarkDirty();
    }

    public asObservable() {
        return this.competitionSubject.asObservable();
    }

    public hasCompetition() {
        return this.competitionSubject != undefined;
    }

    public update(competition: RestCompetitionDescription) {
        this.competitionSubject.next(competition);
        this.markDirty();
    }

    public updateTask(task: RestTaskDescription){
        const idx = this.competitionSubject.getValue().tasks.findIndex(t => t.id === task.id)
        this.competitionSubject.getValue().tasks[idx] = task;
        this.markDirty()
    }

    public get() {
        return this.competitionSubject.getValue();
    }


    public clear() {
        this.unmarkDirty();
        this.competitionSubject = undefined;
    }

    public checkDirty() {
        if (!this.dirty) {
            return true;
        }
        return confirm('There are unsaved changes in this competition that will be lost. Do you really want to proceed?');
    }

    public unmarkDirty() {
        this.dirty = false;
        if (this.shouldLogDirtyChanges) {
            console.log('[CompetitionBuilderService] unmarkDirty (dirty=false)');
        }
    }

    public markDirty() {
        this.dirty = true;
        if (this.shouldLogDirtyChanges) {
            console.log('[CompetitionBuilderService] markDirty (dirty=true)');
        }
    }

    public isDirty() {
        return this.dirty;
    }

    public removeTaskType(taskType: TaskType) {
        this.competitionSubject.getValue().taskTypes.splice(this.competitionSubject.getValue().taskTypes.indexOf(taskType), 1);
        this.competitionSubject.getValue().taskGroups.filter((t) => t.type === taskType.name)
            .forEach((g) => {
                this.removeTaskGroup(g);
            });
        this.competitionSubject.next(this.competitionSubject.getValue());
        this.markDirty();
    }

    public removeTaskGroup(taskGroup: TaskGroup) {
        this.competitionSubject.getValue().taskGroups.splice(this.competitionSubject.getValue().taskGroups.indexOf(taskGroup), 1);
        this.competitionSubject.getValue().tasks.filter((t) => t.taskGroup === taskGroup.name)
            .forEach((t) => {
                this.removeTask(t);
            });
        this.competitionSubject.next(this.competitionSubject.getValue());
        this.markDirty();
    }

    public removeTask(task: RestTaskDescription) {
        this.competitionSubject.getValue().tasks.splice(this.competitionSubject.getValue().tasks.indexOf(task), 1);
        this.competitionSubject.next(this.competitionSubject.getValue());
        this.markDirty();
    }
}
