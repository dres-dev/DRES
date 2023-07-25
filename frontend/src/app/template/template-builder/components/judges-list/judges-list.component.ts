import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {MatTable} from '@angular/material/table';
import { ApiRole, ApiUser, TemplateService, UserService } from "../../../../../../openapi";
import {combineLatest, Observable} from 'rxjs';
import {AbstractTemplateBuilderComponent} from '../abstract-template-builder.component';
import {TemplateBuilderService} from '../../template-builder.service';
import {filter, map, shareReplay, withLatestFrom} from 'rxjs/operators';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";

@Component({
    selector: 'app-judges-list',
    templateUrl: './judges-list.component.html',
    styleUrls: ['./judges-list.component.scss']
})
export class JudgesListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {

    @ViewChild('judgesTable')
    judgesTable: MatTable<ApiUser>;

    availableJudges: Observable<ApiUser[]>;

    displayedColumns: string[] = ['name', 'action'];

    judges: Observable<Array<string>> = new Observable<Array<string>>((x) => x.next([]));

    constructor(private userService: UserService,
                builderService: TemplateBuilderService,
                route: ActivatedRoute,
                templateService: TemplateService,
                snackBar: MatSnackBar,
    ) {
        super(builderService,route,templateService,snackBar);
        this.refreshAvailableJudges();
    }

    addJudge(event: MatAutocompleteSelectedEvent) {
        if (this.builderService.getTemplate().judges.includes(event.option.value.id)) {
            return;
        }
        this.builderService.getTemplate().judges.push(event.option.value.id);
        this.builderService.update();
        this.judgesTable.renderRows();
    }

    removeJudge(judgeId: string) {
        this.builderService.getTemplate().judges.splice(this.builderService.getTemplate().judges.indexOf(judgeId), 1);
        this.builderService.update();
        this.judgesTable.renderRows();
    }

    judgeForId(id: string) {
        return this.availableJudges.pipe(map((users) => users.find((u) => u.id === id)));
    }

    displayJudge(judge: ApiUser) {
        return judge.username;
    }

    ngOnInit(): void {
        this.onInit();
    }

    ngOnDestroy(): void {
        this.onDestroy();
    }

    onChange() {
        this.judges = this.builderService.templateAsObservable().pipe(
            map((t) => {
                if (t) {
                    return t.judges;
                } else {
                    return [];
                }
            })
        );
    }

    refreshAvailableJudges() {
        this.availableJudges = this.userService.getApiV2UserList().pipe(
            map((users) => users.filter((user) => user.role === ApiRole.JUDGE)),
            /*withLatestFrom(this.judges),
            map(([users, judges]) => {
               return users.filter((u) => !judges.includes(u.id))
            }),*/
            shareReplay(1)
        );
    }

}
