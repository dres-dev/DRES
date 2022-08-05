import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {Subscription} from 'rxjs';
import {ActivatedRoute} from '@angular/router';
import {CompetitionService, RestCompetitionDescription} from '../../../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {AbstractCompetitionBuilderComponent} from '../shared/abstract-competition-builder.component';
import {CompetitionBuilderService} from '../../competition-builder.service';

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
      builderService: CompetitionBuilderService
  ) {
    super(builderService)
  }

  ngOnInit(): void {
    this.onInit();

    this.changeSubscription = this.form.valueChanges.subscribe(() => {
      this.builderService.markDirty()
    });
  }

  onChange(competition: RestCompetitionDescription) {
    if(competition){
      this.form.get('name').setValue(this.competition.name);
      this.form.get('description').setValue(this.competition.description);
    }
  }

  ngOnDestroy() {
    this.onDestroy()
  }


}
