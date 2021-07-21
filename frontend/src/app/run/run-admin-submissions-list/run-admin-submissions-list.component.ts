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

    @ViewChild('group', {static: true}) group: MatButtonToggleGroup;

    @ViewChild('table', {static: true}) table: MatTable<SubmissionInfo>;

    @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;

    /** ID of the competition run shown by this {@link RunAdminSubmissionsListComponent}*/
    public competitionRunId: string;

    /** ID of the task shown by this {@link RunAdminSubmissionsListComponent}*/
    public taskId: string;

    /** The columns displayed by the table. */
    public displayColumns = ['id', 'timestamp', 'submitted', 'item', 'start', 'end', 'status', 'actions'];

    /** Number of milliseconds to wait in between polls. */
    public pollingFrequencyFactor = 30000; // every 30 seconds

    /** Flag indicating whether list of submissions should be polled. */
    public polling = true;

    /** Flag indicating whether information about the submitter should be anonymized. */
    public anonymize = true;

    /** Subject used to manually trigger a refresh. */
    public refreshSubject: Subject<null> = new Subject();

    /** The data source for the table. */
    public dataSource: MatTableDataSource<SubmissionInfo> = new MatTableDataSource();


    /** Subscription held by this component to load submissions. */
    private subscription: Subscription;

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

    /**
     *
     * @param submission
     */
    public showDetails(submission: SubmissionInfo) {
        this.snackBar.open(`Team: ${submission.teamName}, Member: ${submission.memberName}`, null, {duration: 5000});
    }
}
