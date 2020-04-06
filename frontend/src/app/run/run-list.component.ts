import {Component} from '@angular/core';
import {CompetitionInfo, CompetitionRunAdminService, CompetitionRunService} from '../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {merge, Observable, Subject, timer} from 'rxjs';
import {combineAll, mergeAll, switchMap} from 'rxjs/operators';

@Component({
    selector: 'app-run-list',
    templateUrl: './run-list.component.html'
})
export class RunListComponent {

    displayedColumns = ['actions', 'id', 'name', 'status', 'currentTask', 'timeLeft', 'description', 'teamCount'];

    update = new Subject();
    runs: Observable<CompetitionInfo[]>;

    constructor(private runService: CompetitionRunService,
                private runAdminService: CompetitionRunAdminService,
                private snackBar: MatSnackBar) {

        this.runs = merge(
            timer(0, 5000),
            this.update
        ).pipe(switchMap(() => this.runService.getApiRun()));
    }

    public start(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidStart(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public terminate(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTerminate(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public nextTask(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTaskNext(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public previousTask(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTaskPrevious(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public startTask(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTaskStart(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public abortTask(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTaskAbort(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }


    public normalizeTimeLeft(timeLeft: number) {
        return timeLeft > -1 ? `${Math.round(timeLeft / 1000)}s` : 'n/a';
    }
}
