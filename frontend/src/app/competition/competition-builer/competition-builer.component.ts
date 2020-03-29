import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-competition-builer',
  templateUrl: './competition-builer.component.html',
  styleUrls: ['./competition-builer.component.scss']
})
export class CompetitionBuilerComponent implements OnInit, OnDestroy {

  competitionId: number;
  private subscription: any;

  constructor(private route: ActivatedRoute,
              private routerService: Router) { }

  ngOnInit() {
    this.subscription = this.route.params.subscribe(params => {
      this.competitionId = +params['competitionId'];
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }


  public back() {
    this.routerService.navigate(['/competition/list']);
  }
}
