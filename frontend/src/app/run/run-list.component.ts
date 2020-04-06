import {Component, OnInit} from '@angular/core';
import {CompetitionInfo, CompetitionRunAdminService, CompetitionRunService} from '../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
    selector: 'app-run-list',
    templateUrl: './run-list.component.html'
})
export class RunListComponent implements OnInit {

    displayedColumns = ['actions', 'id', 'name', 'status', 'currentTask', 'description', 'teamCount'];
    runs: CompetitionInfo[] = [];

    constructor(private runService: CompetitionRunService,
                private runAdminService: CompetitionRunAdminService,
                private snackBar: MatSnackBar) {
    }

    ngOnInit(): void {
        this.refresh();
    }

    public start(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidStart(runId.toString()).subscribe(
            (r) => {
                this.refresh();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public terminate(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTerminate(runId.toString()).subscribe(
            (r) => {
                this.refresh();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public nextTask(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTaskNext(runId.toString()).subscribe(
            (r) => {
                this.refresh();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public previousTask(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTaskPrevious(runId.toString()).subscribe(
            (r) => {
                this.refresh();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public startTask(runId: number) {
        this.runAdminService.postApiRunAdminWithRunidTaskStart(runId.toString()).subscribe(
            (r) => {
                this.refresh();
                this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }

    public refresh() {
        this.runService.getApiRun().subscribe(
            (results: CompetitionInfo[]) => {
                this.runs = results;
            },
            (r) => {
                this.runs = [];
                this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
            }
        );
    }
}
