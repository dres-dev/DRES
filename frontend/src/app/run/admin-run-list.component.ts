import {Component} from '@angular/core';
import {AbstractRunListComponent} from './abstract-run-list.component';
import {CompetitionRunAdminService, CompetitionRunScoresService, CompetitionRunService} from '../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {ConfirmationDialogComponent, ConfirmationDialogComponentData} from '../shared/confirmation-dialog/confirmation-dialog.component';

@Component({
    selector: 'app-admin-run-list',
    templateUrl: './admin-run-list.component.html'
})
export class AdminRunListComponent extends AbstractRunListComponent {
    constructor(runService: CompetitionRunService,
                runAdminService: CompetitionRunAdminService,
                scoreService: CompetitionRunScoresService,
                router: Router,
                private snackBar: MatSnackBar,
                private dialog: MatDialog) {
        super(runService, runAdminService, scoreService, router);
    }

    public start(runId: string) {
        this.runAdminService.postApiRunAdminWithRunidStart(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public terminate(runId: string) {
        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            data: {
                text: 'You are about to terminate this run. This action cannot be undone. Do you want to proceed?',
                color: 'warn'
            } as ConfirmationDialogComponentData
        });
        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.runAdminService.postApiRunAdminWithRunidTerminate(runId).subscribe(
                    (r) => {
                        this.update.next();
                        this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
                    }, (r) => {
                        this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                    }
                );
            }
        });
    }

    public nextTask(runId: string) {
        this.runAdminService.postApiRunAdminWithRunidTaskNext(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public previousTask(runId: string) {
        this.runAdminService.postApiRunAdminWithRunidTaskPrevious(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public startTask(runId: string) {
        this.runAdminService.postApiRunAdminWithRunidTaskStart(runId).subscribe(
            (r) => {
                this.update.next();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
        );
    }

    public abortTask(runId: string) {
        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            data: {
                text: 'Really abort the task?',
                color: 'warn'
            } as ConfirmationDialogComponentData
        });
        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.runAdminService.postApiRunAdminWithRunidTaskAbort(runId).subscribe(
                    (r) => {
                        this.update.next();
                        this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
                    }, (r) => {
                        this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
                    }
                );
            }
        });
    }
}
