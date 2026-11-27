import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatTable } from '@angular/material/table';
import { ActivatedRoute } from '@angular/router';
import { combineLatest, Observable, startWith } from 'rxjs';
import { filter, map, shareReplay, tap, withLatestFrom } from 'rxjs/operators';
import { ApiRole, ApiUser, TemplateService, UserService } from '../../../../../../openapi';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AbstractTemplateBuilderComponent } from '../abstract-template-builder.component';
import { TemplateBuilderService } from '../../template-builder.service';

@Component({
  selector: 'app-judges-list',
  templateUrl: './judges-list.component.html',
  styleUrls: ['./judges-list.component.scss'],
  standalone: false,
})
export class JudgesListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {
  @ViewChild('judgesTable')
  judgesTable: MatTable<ApiUser>;

  allJudges: Observable<ApiUser[]>;
  availableJudges: Observable<ApiUser[]>;
  judgeSearch = new FormControl<string | ApiUser>('');
  displayedColumns: string[] = ['name', 'action'];
  judges: Observable<Array<string>> = new Observable<Array<string>>((x) => x.next([]));

  constructor(
    private userService: UserService,
    builderService: TemplateBuilderService,
    route: ActivatedRoute,
    templateService: TemplateService,
    snackBar: MatSnackBar
  ) {
    super(builderService, route, templateService, snackBar);
    this.refreshAvailableJudges();
  }

  addJudge(event: MatAutocompleteSelectedEvent) {
    if (this.builderService.getTemplate().judges.includes(event.option.value.id)) {
      return;
    }
    this.builderService.getTemplate().judges.push(event.option.value.id);
    this.judgeSearch.setValue('');
    this.builderService.update();
    this.judgesTable.renderRows();
  }

  removeJudge(judgeId: string) {
    const idx = this.builderService.getTemplate().judges.indexOf(judgeId);
    if (idx < 0) {
      return;
    }
    this.builderService.getTemplate().judges.splice(idx, 1);
    this.builderService.update();
    this.judgesTable.renderRows();
  }

  judgeForId(id: string) {
    return this.allJudges.pipe(map((users) => users.find((u) => u.id === id)));
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
      }),
      tap((_) => this.judgesTable?.renderRows())
    );
  }

  refreshAvailableJudges() {
    this.allJudges = this.userService.getApiV2UserList().pipe(
      map((users) => users.filter((user) => user.role === ApiRole.JUDGE)),
      shareReplay(1)
    );
    this.availableJudges = combineLatest([
      this.allJudges,
      this.judgeSearch.valueChanges.pipe(startWith(this.judgeSearch.value)),
      this.builderService.templateAsObservable(),
    ]).pipe(
      map(([users, search, template]) => {
        const filterValue = typeof search === 'string' ? search.toLowerCase() : '';
        const assigned = template?.judges ?? [];
        return users
          .filter((u) => !assigned.includes(u.id))
          .filter((u) => !filterValue || u.username.toLowerCase().includes(filterValue));
      })
    );
  }
}
