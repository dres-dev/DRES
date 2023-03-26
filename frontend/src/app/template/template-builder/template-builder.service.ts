import { Injectable } from '@angular/core';
import {ApiEvaluationTemplate, ApiTaskGroup, ApiTaskTemplate, ApiTaskType} from '../../../../openapi';
import { BehaviorSubject, Observable } from "rxjs";
import { map } from "rxjs/operators";


/**
 * A service to manage the currently actively edited evaluation template.
 * The service provides the means to modify the template as needed and orchestrates updates.
 */
@Injectable({
  providedIn: 'root'
})
export class TemplateBuilderService {
  get selectedTaskGroup(): ApiTaskGroup {
    return this._selectedTaskGroup;
  }

  get selectedTaskType(): ApiTaskType {
    return this._selectedTaskType;
  }


  // TODO might be worthwhile to be the sole provider for a template, i.e. fetching the template from the API would be handled here as well...

  private shouldLogDirtyChanges = true;
  private templateSubject: BehaviorSubject<ApiEvaluationTemplate> = new BehaviorSubject<ApiEvaluationTemplate>(null);
  private dirtySubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  private selectedTaskTemplate: BehaviorSubject<ApiTaskTemplate> = new BehaviorSubject<ApiTaskTemplate>(null);
  private _selectedTaskType: ApiTaskType;
  private _selectedTaskGroup: ApiTaskGroup;

  constructor() {
  }

  public selectTaskTemplate(task: ApiTaskTemplate){
    console.log("BuilderService.selectTaskTemplate task", task);
    console.log("BuilderService.selectTaskTemplate template's tasks", this.getTemplate().tasks);
    if(task){
      const index = this.getTemplate().tasks.indexOf(task);
      console.log("BuilderService.selectTaskTemplate, index", index);
      if(index < 0){
        console.log("BuilderService.selectTaskTemplate, NEW Task");
        /* new task: we'll have to add id */
        this.getTemplate().tasks.push(task);
        this.update(this.getTemplate());
      }
      this._selectedTaskGroup = this.findGroupByName(task.taskGroup);
      this._selectedTaskType = this.findTypeByName(task.taskType);
      this.selectedTaskTemplate.next(task);
    }else{
      console.log("BuilderService.selectTaskTemplate UNSELECT");
      this.selectedTaskTemplate.next(null);
      this._selectedTaskGroup = null;
      this._selectedTaskType = null;
    }
  }

  public selectedTaskTemplateAsObservable(){
    return this.selectedTaskTemplate.asObservable();
  }

  public getSelectedTaskTemplate(){
    return this.selectedTaskTemplate.getValue();
  }

  public findTypeForGroup(group: ApiTaskGroup){
    return this.getTemplate().taskTypes.find((v) => v.name === group.type);
  }

  public findGroupByName(name: string){
    return this.getTemplate().taskGroups.find((v) => v.name === name);
  }

  public findTypeByName(name: string){
    return this.getTemplate().taskTypes.find((v) => v.name === name);
  }

  public initialise(template: ApiEvaluationTemplate){
    this.unmarkDirty();
    this.templateSubject.next(template);
    console.log("BuilderService.init", template);
  }

  public getTemplate(){
    return this.templateSubject.getValue();
  }

  /**
   * @deprecated
   */
  public getTemplateCleaned(){
    const template = this.templateSubject.getValue();
    return template;
  }

  public templateAsObservable(){
    return this.templateSubject.asObservable();
  }

  public taskTemplatesAsObservable(): Observable<ApiTaskTemplate[]>{
    return this.templateAsObservable().pipe(map((t) => {
      if(t){
        return t.tasks;
      }else{
        return [];
      }
    }));
  }

  public taskTypesAsObservable(): Observable<ApiTaskType[]>{
    return this.templateAsObservable().pipe(map((t) => {
      if(t){
        return t.taskTypes;
      }else{
        return [];
      }
    }));
  }

  public taskGroupsAsObservable(): Observable<ApiTaskGroup[]>{
    return this.templateAsObservable().pipe(map((t) => {
      if(t){
        return t.taskGroups;
      }else{
        return [];
      }
    }))
  }

  public update(template: ApiEvaluationTemplate = null){
    template = template ? template : this.templateSubject.getValue();
    console.log("BuilderService.update", template)
    this.templateSubject.next(template);
    this.markDirty();
  }

  public updateTask(task: ApiTaskTemplate){
    console.log('update task', task);
    console.log('update task, all', this.getTemplate().tasks);
    const idx = this.getTemplate().tasks.indexOf(task);
    console.log('update task, index', idx);
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
    this.selectedTaskTemplate.next(null);
    this._selectedTaskType = null;
    this._selectedTaskGroup = null;
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
