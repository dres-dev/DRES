import {AfterViewInit, Component, OnDestroy, ViewChild} from '@angular/core';
import {CompetitionRunAdminService, SubmissionInfo} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {MatTable, MatTableDataSource} from '@angular/material/table';
import {merge, Observable, of, Subject, Subscription, timer} from 'rxjs';
import {ActivatedRoute} from '@angular/router';
import {MatButtonToggleGroup} from '@angular/material/button-toggle';
import {catchError, filter, map, switchMap, withLatestFrom} from 'rxjs/operators';
import {MatPaginator} from '@angular/material/paginator';
import {AppConfig} from '../../app.config';

@Component({
    selector: 'app-run-admin-submissions-list',
    templateUrl: './run-admin-submissions-list.component.html',
    styleUrls: ['./run-admin-submissions-list.component.scss']
})
export class RunAdminSubmissionsListComponent implements AfterViewInit, OnDestroy {

    @ViewChild('group', {static: true}) group: MatButtonToggleGroup;

    @ViewChild('table', {static: true}) table: MatTable<SubmissionInfo>;

    @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;

    /** ID of the competition run shown by this {@link RunAdminSubmissionsListComponent}. */
    public runId: Observable<string>;

    /** ID of the task shown by this {@link RunAdminSubmissionsListComponent}. */
    public taskId: Observable<string>;

    /** The columns displayed by the table. */
    public displayColumns = ['id', 'taskRunId', 'timestamp', 'submitted', 'item', 'start', 'end', 'status', 'preview', 'actions'];

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
        private dialog: MatDialog,
        private activeRoute: ActivatedRoute,
        private runService: CompetitionRunAdminService,
        public config: AppConfig
    ) {
        this.runId = this.activeRoute.paramMap.pipe(map(params => params.get('runId')));
        this.taskId = this.activeRoute.paramMap.pipe(map(params => params.get('taskId')));
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
            withLatestFrom(this.runId, this.taskId),
            switchMap(([i, r, t]) => this.runService.getApiV1RunAdminWithRunidSubmissionListWithTaskid(r, t)),
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

    /**
     * Updates the status of a Submission
     *
     * @param submission The {@link SubmissionInfo} to update.
     * @param newStatus The new status.
     */
    public update(submission: SubmissionInfo, newStatus: SubmissionInfo.StatusEnum) {
        submission.status = newStatus;
        console.log(submission);
        this.runId.pipe(switchMap(runId => this.runService.patchApiV1RunAdminWithRunidSubmissionOverride(runId, submission))).subscribe(res => {
            this.snackBar.open(`Submission ${res.id} successfully updated to ${res.status}.`, null, {duration: 5000});
        });
    }


    /**
     * Generates a URL for the preview image of a submission.
     */
    public previewForSubmission(submission: SubmissionInfo): Observable<string> {
        return this.runId.pipe(map(runId => this.config.resolveApiUrl(`/preview/submission/${runId}/${submission.id}`)));
    }
}
