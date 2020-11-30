import {AfterViewInit, Component, OnDestroy, ViewChild} from '@angular/core';
import {CompetitionRunAdminService, SubmissionInfo} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {MatTable} from '@angular/material/table';
import {Subscription} from 'rxjs';
import {ActivatedRoute} from '@angular/router';
import {MatButtonToggleGroup} from '@angular/material/button-toggle';

@Component({
    selector: 'app-run-admin-submissions-list',
    templateUrl: './run-admin-submissions-list.component.html',
    styleUrls: ['./run-admin-submissions-list.component.scss']
})
export class RunAdminSubmissionsListComponent implements AfterViewInit, OnDestroy {

    /**
     * The base polling frequency of this polling is every half second
     */
    static readonly BASE_POLLING_FREQUENCY = 1000; // ms -> 1s


    public competitionRunId: string;
    public taskId: string;

    @ViewChild('group', {static: true}) group: MatButtonToggleGroup;

    @ViewChild('table', {static: true}) table: MatTable<SubmissionInfo>;
    /**
     * The displayed columns
     */
    displayColumns = ['id', 'timestamp', 'status', 'item', 'actions'];

    submissions: SubmissionInfo[] = [];

    pollingFrequencyFactor = 60; // every 60 seconds


    private pollingSub: Subscription;

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

    ngAfterViewInit(): void {
        // this.pollingSub = interval(this.pollingFrequencyFactor * RunAdminSubmissionsListComponent.BASE_POLLING_FREQUENCY)
        //     .subscribe(_ => {
        this.refresh();
        // });
    }

    ngOnDestroy(): void {
        if (this.pollingSub) {
            this.pollingSub.unsubscribe();
        }
        this.submissions = [];
    }

    update(submission: SubmissionInfo, status: SubmissionInfo.StatusEnum) {
        submission.status = status;
        console.log(submission);
        this.runService.patchApiRunAdminWithRunidSubmissionsOverride(this.competitionRunId, submission).subscribe(res => {
            this.snackBar.open(`Result: ${res}`, null, {duration: 5000});
        });
    }

    public refresh() {
        this.runService.getApiRunAdminWithRunidSubmissionsListWithTaskid(this.competitionRunId, this.taskId)
            .subscribe(subs => {
                    this.submissions = subs;
                },
                (error) => {
                    this.submissions = [];
                    this.snackBar.open(`Error: ${error.error.description}`, null, {duration: 5000});
                    console.error(error);
                });
    }
}
