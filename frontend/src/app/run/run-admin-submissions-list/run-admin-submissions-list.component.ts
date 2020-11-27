import {AfterViewInit, Component, OnDestroy, ViewChild} from '@angular/core';
import {CompetitionRunAdminService, SubmissionInfo} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {MatTable} from '@angular/material/table';
import {interval, Subscription} from 'rxjs';
import {ActivatedRoute} from '@angular/router';

@Component({
    selector: 'app-run-admin-submissions-list',
    templateUrl: './run-admin-submissions-list.component.html',
    styleUrls: ['./run-admin-submissions-list.component.scss']
})
export class RunAdminSubmissionsListComponent implements AfterViewInit, OnDestroy {

    /**
     * The base polling frequency of this polling is every half second
     */
    static readonly BASE_POLLING_FREQUENCY = 500; // ms -> .5s

    public readonly SUBMISSION_STATUS = Object.keys(SubmissionInfo.StatusEnum);

    public competitionRunId: string;
    public taskId: string;

    @ViewChild('table', {static: true}) table: MatTable<SubmissionInfo>;
    /**
     * The displayed columns
     */
    displayColumns = ['id', 'timestamp', 'team', 'status', 'item', 'actions'];

    submissions: SubmissionInfo[] = [];

    pollingFrequencyFactor = 1; // every half second, will be multiplied with BASE_POLLING_FREQUENCY


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
        this.pollingSub = interval(this.pollingFrequencyFactor * RunAdminSubmissionsListComponent.BASE_POLLING_FREQUENCY)
            .subscribe(_ => {
                this.runService.getApiRunAdminWithRunidSubmissionsListWithTaskid(this.competitionRunId, this.taskId)
                    .subscribe(subs => {
                        if (subs.length > 1) {
                            subs.forEach(s => this.submissions.push(s));
                            if (this.table) {
                                this.table.renderRows();
                            }
                        }
                    });
            });
    }

    ngOnDestroy(): void {
        this.pollingSub.unsubscribe();
        this.submissions = [];
    }


}
