import { Injectable } from '@angular/core';
import {ApiEvaluationTemplate, ApiTaskGroup, ApiTaskTemplate, ApiTaskType} from '../../../../openapi';
import {BehaviorSubject} from 'rxjs';


/**
 * A service to manage the currently actively edited evaluation template.
 * The service provides the means to modify the template as needed and orchestrates updates.
 */
@Injectable({
  providedIn: 'root'
})
export class TemplateBuilderService {

  // TODO might be worthwhile to be the sole provider for a template, i.e. fetching the template from the API would be handled here as well...

  private shouldLogDirtyChanges = true;
  private templateSubject: BehaviorSubject<ApiEvaluationTemplate> = new BehaviorSubject<ApiEvaluationTemplate>(null);
  private dirtySubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  constructor() {
  }

  public initialise(template: ApiEvaluationTemplate){
    this.unmarkDirty();
    this.templateSubject.next(template);
  }

  public getTemplate(){
    return this.templateSubject.getValue();
  }

  public getTemplateCleaned(){
    const template = this.templateSubject.getValue();
    console.log(template.tasks);
    template.tasks.filter((t) => t.id.startsWith('_')).forEach((t) => t.id = '');
    return template;
  }

  public templateAsObservable(){
    return this.templateSubject.asObservable();
  }

  public update(template: ApiEvaluationTemplate = null){
    template = template ? template : this.templateSubject.getValue();
    this.templateSubject.next(template);
    this.markDirty();
  }

  public updateTask(task: ApiTaskTemplate){
    console.log('update task', task);
    if(this.getTemplate().tasks == null || this.getTemplate().tasks.length === 0){
      this.templateSubject.getValue().tasks.push(task);
    }
    const idx = this.getTemplate().tasks.findIndex(t => t.id === task.id);
    this.templateSubject.getValue().tasks[idx] = task;
    this.update(this.getTemplate());
    this.markDirty();
  }

  public hasTemplate(){
    return this.templateSubject != undefined && this.templateSubject.getValue();
  }

  public clear(){
    this.unmarkDirty();
    this.templateSubject.unsubscribe();
    this.templateSubject = undefined;
  }

  public checkDirty(){
    if(!this.dirtySubject.value){
      return true;
    }
    return confirm('There are unsaved changes in this evaluation template that will be lost. Do you really want to proceed?')
  }

  public markDirty(){
    this.dirtySubject.next(true);
  }

  public unmarkDirty(){
    this.dirtySubject.next(false);
  }

  public isDirty(){
    return this.dirtySubject.value;
  }

  public dirty(){
    return this.dirtySubject.asObservable();
  }

  public removeTaskType(taskType: ApiTaskType){
    this.getTemplate().taskTypes.splice(this.getTemplate().taskTypes.indexOf(taskType), 1);
    this.getTemplate().taskGroups.filter((g) => g.type === taskType.name)
        .forEach((g) => this.removeTaskGroup(g));
    this.update(this.getTemplate())
  }

  public removeTaskGroup(taskGroup: ApiTaskGroup){
    this.getTemplate().taskGroups.splice(this.getTemplate().taskGroups.indexOf(taskGroup), 1);
    this.getTemplate().tasks.filter((t) => t.taskGroup === taskGroup.name)
        .forEach((t) => this.removeTask(t));
    this.update(this.getTemplate());
  }

  public removeTask(task: ApiTaskTemplate){
    this.getTemplate().tasks.splice(this.getTemplate().tasks.indexOf(task), 1);
    this.update(this.getTemplate());
  }
}
