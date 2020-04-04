import {Component, OnInit} from '@angular/core';
import {CompetitionInfo, CompetitionRunService} from '../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
    selector: 'app-run-list',
    templateUrl: './run-list.component.html'
})
export class RunListComponent implements OnInit {

    displayedColumns = ['actions', 'id', 'name', 'status', 'description', 'teamCount'];
    runs: CompetitionInfo[] = [];

    constructor(private runService: CompetitionRunService,
                private snackBar: MatSnackBar) {
    }

    ngOnInit(): void {
        this.runService.getApiRun().subscribe(
        (results: CompetitionInfo[]) => {
            this.runs = results;
        },
        (r) => {
            this.runs = [];
            this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
        });
    }
}
