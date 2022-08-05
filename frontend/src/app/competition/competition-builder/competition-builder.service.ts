import { Injectable } from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {RestCompetitionDescription} from '../../../../openapi';


/**
 * A service to share the currently editing competition among the competition builder components
 */
@Injectable({
  providedIn: 'root'
})
export class CompetitionBuilderService {


  private competitionSubject:BehaviorSubject<RestCompetitionDescription> = new BehaviorSubject<RestCompetitionDescription>(null)
  private dirty = false;

  constructor() { }

  public initialise(competition: RestCompetitionDescription){
    this.competitionSubject.next(competition)
    this.unmarkDirty()
  }

  public asObservable(){
    return this.competitionSubject.asObservable()
  }

  public hasCompetition(){
    return this.competitionSubject != undefined
  }

  public update(competition: RestCompetitionDescription){
    this.competitionSubject.next(competition)
    this.markDirty()
  }

  public get(){
    return this.competitionSubject.getValue()
  }

  public clear(){
    this.unmarkDirty()
    this.competitionSubject = undefined;
  }

  public checkDirty(){
    if(!this.dirty){
      return true;
    }
    return confirm('There are unsaved changes in this competition that will be lost. Do you really want to proceed?');
  }

  public unmarkDirty(){
    this.dirty = false;
    console.log('dirty = false')
  }

  public markDirty(){
    this.dirty = true;
    console.log('dirty = true')
  }

  public isDirty(){
    return this.dirty;
  }
}
