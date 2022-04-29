import { AfterViewInit, Component } from '@angular/core';
import {
  CompetitionCreate,
  CompetitionOverview,
  CompetitionRunAdminService,
  CompetitionService,
  CompetitionStartMessage,
  DownloadService,
  RunProperties,
} from '../../../../openapi';
import {MatDialog} from '@angular/material/dialog';
import {CompetitionCreateDialogComponent} from './competition-create-dialog.component';
import {filter, flatMap, take, tap} from 'rxjs/operators';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Router} from '@angular/router';
import {CompetitionStartDialogComponent, CompetitionStartDialogResult} from './competition-start-dialog.component';
import {ConfirmationDialogComponent, ConfirmationDialogComponentData} from '../../shared/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-competition-list',
  templateUrl: './competition-list.component.html',
  styleUrls: ['./competition-list.component.scss'],
})
export class CompetitionListComponent implements AfterViewInit {
  /** */
  displayedColumns = ['actions', 'id', 'name', 'description', 'taskCount', 'teamCount'];
  competitions: CompetitionOverview[] = [];
  waitingForRun = false;

  constructor(
    private competitionService: CompetitionService,
    private runAdminService: CompetitionRunAdminService,
    private downloadService: DownloadService,
    private routerService: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  public create() {
    const dialogRef = this.dialog.open(CompetitionCreateDialogComponent, { width: '500px' });
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        flatMap((r: CompetitionCreate) => {
          return this.competitionService.postApiV1Competition(r);
        })
      )
      .subscribe(
        (r) => {
          this.refresh();
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
        }
      );
  }

  public createRun(id: string) {
    const dialogRef = this.dialog.open(CompetitionStartDialogComponent, { width: '500px' });
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        tap((r) => (this.waitingForRun = true)),
        flatMap((r: CompetitionStartDialogResult) => {
          const properties = { participantCanView: r.participantCanView, shuffleTasks: r.shuffleTasks } as RunProperties;
          return this.runAdminService.postApiV1RunAdminCreate({
            competitionId: id,
            name: r.name,
            type: r.type,
            properties: properties,
          } as CompetitionStartMessage);
        })
      )
      .subscribe(
        (r) => {
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
          this.waitingForRun = false;
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
          this.waitingForRun = false;
        }
      );
  }

  public edit(competitionId: string) {
    this.routerService.navigate(['/competition/builder', competitionId]);
  }

    public delete(competitionId: string) {
        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            data: {
                text: '`Do you really want to delete competition with ID ${competitionId}?`',
                color: 'warn'
            } as ConfirmationDialogComponentData
        });
        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.competitionService.deleteApiV1CompetitionWithCompetitionid(competitionId).subscribe(
                    (r) => {
                        this.refresh();
                        this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
                    },
                    (r) => {
                        this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                    }
                );
            }
        });
    }

  public refresh() {
    this.competitionService.getApiV1CompetitionList().subscribe(
      (results: CompetitionOverview[]) => {
        this.competitions = results;
      },
      (r) => {
        this.competitions = [];
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  ngAfterViewInit(): void {
    this.refresh();
  }

    downloadProvider = (competitionId) => {
        return this.downloadService.getApiV1DownloadCompetitionWithCompetitionid(competitionId)
            .pipe(take(1));
        // .toPromise();
    };

  fileProvider = (name: string) => {
    return () => name;
  };

  resolveCompetitionOverviewById(_: number, item: CompetitionOverview) {
    return item.id;
  }
}
