import {AfterViewInit, Component, OnDestroy, ViewChild} from '@angular/core';
import {CompetitionRunAdminService, SubmissionInfo} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {MatTable, MatTableDataSource} from '@angular/material/table';
import {merge, of, Subject, Subscription, timer} from 'rxjs';
import {ActivatedRoute} from '@angular/router';
import {MatButtonToggleGroup} from '@angular/material/button-toggle';
import {catchError, filter, switchMap} from 'rxjs/operators';
import {MatPaginator} from '@angular/material/paginator';

@Component({
    selector: 'app-run-admin-submissions-list',
    templateUrl: './run-admin-submissions-list.component.html',
    styleUrls: ['./run-admin-submissions-list.component.scss']
})
export class RunAdminSubmissionsListComponent implements AfterViewInit, OnDestroy {

    public competitionRunId: string;
    public taskId: string;

    @ViewChild('group', {static: true}) group: MatButtonToggleGroup;

    @ViewChild('table', {static: true}) table: MatTable<SubmissionInfo>;

    @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;

    refreshSubject: Subject<null> = new Subject();

    /**
     * The displayed columns
     */
    displayColumns = ['id', 'timestamp', 'team', 'item', 'start', 'end', 'status', 'actions'];
    pollingFrequencyFactor = 30000; // every 30 seconds
    polling = true;
    subscription: Subscription;
    dataSource: MatTableDataSource<SubmissionInfo> = new MatTableDataSource();

    constructor(
        private snackBar: MatSnackBar,
        private runService: CompetitionRunAdminService,
        private dialog: MatDialog,
        private activeRoute: ActivatedRoute,
    ) {
        this.activeRoute.paramMap.subscribe(params => {
            this.competitionRunId = params.get('runId');
            this.taskId = params.get('taskId');
        });
    }

    /**
     * Register subscription for submission data;
     *
     * TODO: In this implementation, pagination is done on the client side!
     */
    ngAfterViewInit() {
        this.dataSource.paginator = this.paginator;
        this.subscription = merge(
            timer(0, this.pollingFrequencyFactor).pipe(filter(i => this.polling)),
            this.refreshSubject
        ).pipe(
            switchMap(s => this.runService.getApiRunAdminWithRunidSubmissionsListWithTaskid(this.competitionRunId, this.taskId)),
            catchError((err, o) => {
                console.log(`[RunAdminSubmissionListComponent] Error occurred while loading submissions: ${err?.message}`);
                this.snackBar.open(`Error: ${err?.message}`, null, {duration: 5000});
                return of([]);
            })
        ).subscribe(s => {
            this.dataSource.data = s;
        });
    }

    /**
     * House keeping; clean up subscriptions.
     */
    ngOnDestroy(): void {
        this.subscription.unsubscribe();
        this.subscription = null;
    }

    update(submission: SubmissionInfo, status: SubmissionInfo.StatusEnum) {
        submission.status = status;
        console.log(submission);
        this.runService.patchApiRunAdminWithRunidSubmissionsOverride(this.competitionRunId, submission).subscribe(res => {
            this.snackBar.open(`Result: ${res}`, null, {duration: 5000});
        });
    }
}
