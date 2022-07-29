import { Component, OnInit } from '@angular/core';
import {Subscription} from 'rxjs';
import {ActivatedRoute} from '@angular/router';
import {CompetitionService, RestCompetitionDescription} from '../../../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Refreshable} from './refreshable.interface';

export class RouteBasedCompetitionAwareComponent implements Refreshable{

  dirty = false;
  routeSubscription: Subscription;
  changeSubscription: Subscription;
  competitionId: string;
  competition: RestCompetitionDescription;

  constructor(
      private route: ActivatedRoute,
      private competitionService: CompetitionService,
      private snackBar: MatSnackBar
  ) { }

  onInit(): void {
    this.routeSubscription = this.route.params.subscribe(p => {
      this.competitionId = p.competitionId;
      this.refresh();
    });
    /*this.changeSubscription = this.form.valueChanges.subscribe(() => {
      this.dirty = true;
    });*/
  }

  public refresh() {
    if (this.checkDirty()) {
      this.competitionService.getApiV1CompetitionWithCompetitionid(this.competitionId).subscribe(
          (c) => {
            this.competition = c;
            // TODO fetch other stuff
            this.dirty = false;
          },
          (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
          }
      );
    }
  }

  private checkDirty(): boolean {
    if (!this.dirty) {
      return true;
    }
    return confirm('There are unsaved changes in this competition that will be lost. Do you really want to proceed?');
  }

}
