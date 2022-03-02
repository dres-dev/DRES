import { Component, OnInit } from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {Subscription} from 'rxjs';
import {ActivatedRoute} from '@angular/router';
import {CompetitionService} from '../../../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-general-competition',
  templateUrl: './general-competition.component.html',
  styleUrls: ['./general-competition.component.scss']
})
export class GeneralCompetitionComponent implements OnInit {

  form: FormGroup = new FormGroup({name: new FormControl(''), description: new FormControl('')});
  dirty = false;
  routeSubscription: Subscription;
  changeSubscription: Subscription;
  competitionId: string;

  constructor(
      private route: ActivatedRoute,
      private competitionService: CompetitionService,
      private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.routeSubscription = this.route.params.subscribe(p => {
      this.competitionId = p.competitionId;
      this.refresh();
    });
    this.changeSubscription = this.form.valueChanges.subscribe(() => {
      this.dirty = true;
    });
  }

  public fetchData() {
    return {
      competitionId: this.competitionId,
      name: this.form.get('name').value,
      description: this.form.get('description').value
    };
  }

  public refresh() {
    if (this.checkDirty()) {
      this.competitionService.getApiV1CompetitionWithCompetitionid(this.competitionId).subscribe(
          (c) => {
            this.form.get('name').setValue(c.name);
            this.form.get('description').setValue(c.description);
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
