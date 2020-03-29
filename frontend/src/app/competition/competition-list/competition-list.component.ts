import {AfterViewInit, Component} from '@angular/core';
import {CompetitionOverview, CompetitionService} from '../../../../openapi';
import {MatDialog} from '@angular/material/dialog';
import {CompetitionCreateDialogComponent, CompetitionCreateDialogResult} from './competition-create-dialog.component';
import {filter, flatMap} from 'rxjs/operators';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-competition-list',
  templateUrl: './competition-list.component.html',
  styleUrls: ['./competition-list.component.scss']
})
export class CompetitionListComponent implements AfterViewInit {

  /** */
  displayedColumns = ['actions', 'id', 'name', 'description', 'taskCount', 'teamCount'];
  competitions: CompetitionOverview[] = [];

  constructor(private competitionService: CompetitionService, private dialog: MatDialog, private snackBar: MatSnackBar) {}


  public create() {
    const dialogRef = this.dialog.open(CompetitionCreateDialogComponent, {width: '500px'});
    dialogRef.afterClosed().pipe(
        filter(r => r != null),
        flatMap((r: CompetitionCreateDialogResult) => {
          return this.competitionService.postApiCompetition({name: r.name, description: r.description} as CompetitionOverview);
        })
    ).subscribe((r) => {
        this.refresh();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
    }, (r) => {
       this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
    });
  }

  public delete(competitionId: number) {
    this.competitionService.deleteApiCompetitionWithCompetitionid(competitionId).subscribe(
        (r) => {
          this.refresh();
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000});
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000});
        }
    );
  }

  public refresh() {
    this.competitionService.getApiCompetition().subscribe((results: CompetitionOverview[]) => {
      this.competitions = results;
    },
    () => {

    });
  }

  ngAfterViewInit(): void {
    this.refresh();
  }
}
