import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {Subscription} from 'rxjs';
import {ActivatedRoute} from '@angular/router';
import {CompetitionService} from '../../../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {AbstractCompetitionBuilderComponent} from '../shared/abstract-competition-builder.component';
import {CompetionBuilderService} from '../../competion-builder.service';

@Component({
  selector: 'app-general-competition',
  templateUrl: './general-competition.component.html',
  styleUrls: ['./general-competition.component.scss']
})
export class GeneralCompetitionComponent extends AbstractCompetitionBuilderComponent implements OnInit, OnDestroy {

  form: FormGroup = new FormGroup({name: new FormControl(''), description: new FormControl('')});
  routeSubscription: Subscription;
  changeSubscription: Subscription;
  competitionId: string;

  constructor(
      private route: ActivatedRoute,
      private competitionService: CompetitionService,
      private snackBar: MatSnackBar,
      builderService: CompetionBuilderService
  ) {
    super(builderService)
  }

  ngOnInit(): void {
    this.onInit();
    this.form.get('name').setValue(this.competition.name);
    this.form.get('description').setValue(this.competition.description);
    this.changeSubscription = this.form.valueChanges.subscribe(() => {
      this.builderService.markDirty()
    });

  }

  ngOnDestroy() {
    this.onDestroy()
  }

  /**
   * @deprecated
   */
  public fetchData() {
    return {
      competitionId: this.competitionId,
      name: this.form.get('name').value,
      description: this.form.get('description').value
    };
  }

  /**
   * @deprecated
   */
  public refresh() {
    if (this.builderService.checkDirty()) {
      this.competitionService.getApiV1CompetitionWithCompetitionid(this.competitionId).subscribe(
          (c) => {
            this.form.get('name').setValue(c.name);
            this.form.get('description').setValue(c.description);
            // TODO fetch other stuff
          },
          (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
          }
      );
    }
  }

}
