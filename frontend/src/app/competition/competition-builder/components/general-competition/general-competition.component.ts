import { Component, OnInit } from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {Subscription} from 'rxjs';
import {ActivatedRoute} from '@angular/router';

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
      private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.routeSubscription = this.route.params.subscribe(p => {
      this.competitionId = p.competitionId;
    });
    this.changeSubscription = this.form.valueChanges.subscribe(() => {
      this.dirty = true;
    });
  }

}
