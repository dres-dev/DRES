import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {RestCompetitionDescription, UserDetails, UserRequest, UserService} from '../../../../../../openapi';
import {MatTable} from '@angular/material/table';
import {Observable} from 'rxjs';
import {map, shareReplay} from 'rxjs/operators';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {AbstractCompetitionBuilderComponent} from '../shared/abstract-competition-builder.component';
import {CompetitionBuilderService} from '../../competition-builder.service';
import RoleEnum = UserRequest.RoleEnum;

@Component({
    selector: 'app-judges-list',
    templateUrl: './judges-list.component.html',
    styleUrls: ['./judges-list.component.scss']
})
export class JudgesListComponent extends AbstractCompetitionBuilderComponent implements OnInit, OnDestroy {

    @ViewChild('judgesTable')
    judgesTable: MatTable<UserDetails>;

    availableJudges: Observable<UserDetails[]>;
    displayedColumnsJudges: string[] = ['name', 'action'];
    judges: Observable<Array<string>> =  new Observable<Array<string>>((o) => o.next([]))

    constructor(private userService: UserService,
                builderService: CompetitionBuilderService) {
        super(builderService);
        this.refreshAvailableJudges();
    }

    public judgeFor(id: string): Observable<UserDetails> {
        return this.availableJudges.pipe(map((users) => users.find((u) => u.id === id)));
    }

    public addJudge(event: MatAutocompleteSelectedEvent) {
        if (this.competition.judges.includes(event.option.value.id)) {
            return;
        }
        this.competition.judges.push(event.option.value.id);
        this.update();
        this.judgesTable.renderRows();
    }

    public removeJudge(judgeId: string) {
        this.competition.judges.splice(this.competition.judges.indexOf(judgeId), 1);
        this.update();
        this.judgesTable.renderRows();
    }

    public dispJudge(user: UserDetails) {
        return user.username;
    }

    ngOnInit(): void {
        this.onInit();
    }

    onChange(competition: RestCompetitionDescription) {
        this.judges = new Observable<Array<string>>((o) => {
            if(competition){
                o.next(competition.judges)
            }else{
                o.next([])
            }
        })
    }

    ngOnDestroy() {
        this.onDestroy();
    }

    refreshAvailableJudges() {
        this.availableJudges = this.userService.getApiV1UserList().pipe(
            map((users) => users.filter((user) => user.role === RoleEnum.JUDGE)),
            shareReplay(1)
        );
    }

}
