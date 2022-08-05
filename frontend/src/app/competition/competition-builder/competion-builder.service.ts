import { Injectable } from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {RestCompetitionDescription} from '../../../../openapi';


/**
 * A service to share the currently editing competition among the competition builder components
 */
@Injectable({
  providedIn: 'root'
})
export class CompetionBuilderService {


  private competitionSubject:BehaviorSubject<RestCompetitionDescription>
  private dirty = false;

  constructor() { }

  public initialise(competition: RestCompetitionDescription){
    this.competitionSubject = new BehaviorSubject<RestCompetitionDescription>(competition)
    this.unmarkDirty()
  }

  public asObservable(){
    return this.competitionSubject?.asObservable()
  }

  public hasCompetition(){
    return this.competitionSubject != undefined
  }

  public update(competition: RestCompetitionDescription){
    this.competitionSubject.next(competition)
    this.dirty = true;
  }

  public get(){
    return this.competitionSubject.getValue()
  }

  public clear(){
    this.dirty = false;
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
  }

  public markDirty(){
    this.dirty = true;
  }
}
