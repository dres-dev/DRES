import {AfterViewInit, Component} from '@angular/core';
import {
    CompetitionCreate,
    CompetitionOverview,
    CompetitionRunAdminService,
    CompetitionService,
    CompetitionStart
} from '../../../../openapi';
import {MatDialog} from '@angular/material/dialog';
import {CompetitionCreateDialogComponent} from './competition-create-dialog.component';
import {filter, flatMap, tap} from 'rxjs/operators';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Router} from '@angular/router';
import {CompetitionStartDialogComponent, CompetitionStartDialogResult} from './competition-start-dialog.component';

@Component({
    selector: 'app-competition-list',
    templateUrl: './competition-list.component.html',
    styleUrls: ['./competition-list.component.scss']
})
export class CompetitionListComponent implements AfterViewInit {

    /** */
    displayedColumns = ['actions', 'id', 'name', 'description', 'taskCount', 'teamCount'];
    competitions: CompetitionOverview[] = [];
    waitingForRun = false;

    constructor(private competitionService: CompetitionService,
                private runAdminService: CompetitionRunAdminService,
                private routerService: Router,
                private dialog: MatDialog,
                private snackBar: MatSnackBar) {
    }


    public create() {
        const dialogRef = this.dialog.open(CompetitionCreateDialogComponent, {width: '500px'});
        dialogRef.afterClosed().pipe(
            filter(r => r != null),
            flatMap((r: CompetitionCreate) => {
                return this.competitionService.postApiCompetition(r);
            })
        ).subscribe((r) => {
            this.refresh();
            this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
        }, (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
        });
    }

    public createRun(id: string) {
        const dialogRef = this.dialog.open(CompetitionStartDialogComponent, {width: '500px'});
        dialogRef.afterClosed().pipe(
            filter(r => r != null),
            tap(r => this.waitingForRun = true),
            flatMap((r: CompetitionStartDialogResult) => {
                return this.runAdminService.postApiRunAdminCreate(
                    {competitionId: id, name: r.name, type: r.type, scoreboards: []} as CompetitionStart
                );
            })
        ).subscribe((r) => {
            this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            this.waitingForRun = false;
        }, (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            this.waitingForRun = false;
        });
    }

    public edit(competitionId: string) {
        this.routerService.navigate(['/competition/builder', competitionId]);
    }

    public delete(competitionId: string) {
        if (confirm(`Do you really want to delete competition with ID ${competitionId}?`)) {
            this.competitionService.deleteApiCompetitionWithCompetitionid(competitionId).subscribe(
                (r) => {
                    this.refresh();
                    this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
                },
                (r) => {
                    this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                }
            );
        }
    }

    public refresh() {
        this.competitionService.getApiCompetition().subscribe((results: CompetitionOverview[]) => {
                this.competitions = results;
            },
            (r) => {
                this.competitions = [];
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            });
    }

    ngAfterViewInit(): void {
        this.refresh();
    }
}
