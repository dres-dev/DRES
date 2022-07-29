import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {CompetitionService, RestCompetitionDescription, UserDetails, UserRequest, UserService} from '../../../../../../openapi';
import {MatTable} from '@angular/material/table';
import {Observable, Subscription} from 'rxjs';
import {map, shareReplay} from 'rxjs/operators';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import RoleEnum = UserRequest.RoleEnum;
import {ActivatedRoute} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {RouteBasedCompetitionAwareComponent} from '../shared/route-based-competition-aware.component';

@Component({
  selector: 'app-judges-list',
  templateUrl: './judges-list.component.html',
  styleUrls: ['./judges-list.component.scss']
})
export class JudgesListComponent extends RouteBasedCompetitionAwareComponent implements OnInit{
  competition: RestCompetitionDescription;

  @ViewChild('judgesTable')
  judgesTable: MatTable<UserDetails>;

  availableJudges: Observable<UserDetails[]>;
  displayedColumnsJudges: string[] = ['name', 'action'];

  dirty = false;
  routeSubscription: Subscription;
  changeSubscription: Subscription;
  competitionId: string;

  constructor(private userService: UserService,
              route: ActivatedRoute,
              competitionService: CompetitionService,
              snackBar: MatSnackBar) {
    super(route, competitionService, snackBar);
    this.availableJudges = this.userService.getApiV1UserList().pipe(
      map((users) => users.filter((user) => user.role === RoleEnum.JUDGE)),
      shareReplay(1)
  ); }

  public judgeFor(id: string): Observable<UserDetails> {
    return this.availableJudges.pipe(map((users) => users.find((u) => u.id === id)));
  }

  public addJudge(event: MatAutocompleteSelectedEvent) {
    if (this.competition.judges.includes(event.option.value.id)) {
      return;
    }
    this.competition.judges.push(event.option.value.id);
    // this.dirty = true;
    this.judgesTable.renderRows();
  }

  public removeJudge(judgeId: string) {
    this.competition.judges.splice(this.competition.judges.indexOf(judgeId), 1);
    // this.dirty = true;
    this.judgesTable.renderRows();
  }

  public dispJudge(user: UserDetails) {
    return user.username;
  }

  ngOnInit(): void {
    this.onInit();
  }

}
