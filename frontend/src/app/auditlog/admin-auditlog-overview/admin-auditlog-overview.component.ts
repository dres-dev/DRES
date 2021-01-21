import {AfterViewInit, Component, OnDestroy, ViewChild} from '@angular/core';
import {
    AuditService,
    RestAuditLogEntry,
    RestCompetitionEndAuditLogEntry, RestCompetitionStartAuditLogEntry,
    RestJudgementAuditLogEntry,
    RestLoginAuditLogEntry,
    RestLogoutAuditLogEntry,
    RestSubmissionAuditLogEntry,
    RestTaskEndAuditLogEntry,
    RestTaskModifiedAuditLogEntry,
    RestTaskStartAuditLogEntry
} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {interval, Subscription} from 'rxjs';

@Component({
    selector: 'app-admin-auditlog-overview',
    templateUrl: './admin-auditlog-overview.component.html',
    styleUrls: ['./admin-auditlog-overview.component.scss']
})
export class AdminAuditlogOverviewComponent implements AfterViewInit, OnDestroy {

    static readonly BASE_POLLING_FREQUENCY = 1000; // ms -> 1s

    @ViewChild('table', {static: true}) table;
    displayCols = ['time', 'api', 'type', 'details', 'id']; // TODO clever way to dynamically list things
    logs: RestAuditLogEntry[] = [];

    pollingFrequencyInSeconds = 1; // every second

    private pollingSub: Subscription;
    private lastUpdated = new Date().valueOf();

    constructor(
        private snackBar: MatSnackBar,
        private logService: AuditService
    ) {
    }

    ngAfterViewInit(): void {
        this.initialRequest();
    }

    ngOnDestroy(): void {
        this.pollingSub.unsubscribe();
        this.logs = [];
    }

    public detailsOf(log: RestAuditLogEntry): string {
        switch (log.type) {
            case 'COMPETITION_START':
                const cs = log as RestCompetitionStartAuditLogEntry;
                return `Competition ${cs.competition} has been started by user ${cs.user}`;
            case 'COMPETITION_END':
                const ce = log as RestCompetitionEndAuditLogEntry;
                return `Competition ${ce.competition} has been ended by user ${ce.user}`;
            case 'TASK_START':
                const ts = log as RestTaskStartAuditLogEntry;
                return `Task ${ts.taskName} in competition ${ts.competition} has been started by user ${ts.user}`;
            case 'TASK_MODIFIED':
                const tm = log as RestTaskModifiedAuditLogEntry;
                return `Task ${tm.taskName} in competition ${tm.competition} has been modified by user ${tm.user}: ${tm.modification}`;
            case 'TASK_END':
                const te = log as RestTaskEndAuditLogEntry;
                return `Task ${te.taskName} in competition ${te.competition} has been ended by user ${te.user}`;
            case 'SUBMISSION':
                const subm = log as RestSubmissionAuditLogEntry;
                return `For ${subm.taskName}, a submission ${JSON.stringify(subm.submission)} was made in competition ${subm.competition}
                 by user ${subm.user} from ${subm.address}`.replace('\n', '');
            case 'JUDGEMENT':
                const judgement = log as RestJudgementAuditLogEntry;
                return `Judge ${judgement.user ? judgement.user : ''} published verdict ${judgement.verdict} for token ${judgement.token}
                 based on validator ${judgement.validator} in competition ${judgement.competition}`.replace('\n', '');
            case 'LOGIN':
                const login = log as RestLoginAuditLogEntry;
                return `${login.user} has logged in using ${login.session}`;
            case 'LOGOUT':
                const logout = log as RestLogoutAuditLogEntry;
                return `${logout.session} was logged out`;
            default:
                return JSON.stringify(log);
        }
    }

    private initialRequest() {
        this.logService.getApiAuditListWithLimitWithPage(1000, 0).subscribe(logs => {
            this.lastUpdated = new Date().valueOf();
            logs.forEach(l => { // Apparently, there is no addAll
                this.logs.push(l);
            });
            if (this.table) {
                this.table.renderRows();
            }
            this.initPolling(); // Start polling, when initial request was performed
        });
    }

    private initPolling() {
        // Poll in polling frequency, in future version frequency is configurable
        this.pollingSub = interval(this.pollingFrequencyInSeconds *
            AdminAuditlogOverviewComponent.BASE_POLLING_FREQUENCY)
            .subscribe(_ => {
                    // Get logs since last update (could be initial, or other), upto in one hour (basically all)
                    this.logService.getApiAuditLogsWithSinceWithUpto(this.lastUpdated, this.upto()).subscribe(logs => {
                        this.lastUpdated = new Date().valueOf();
                        if (logs.length > 1) { // Still no addAll
                            logs.forEach(l => {
                                this.logs.push(l);
                            });
                            if (this.table) {
                                this.table.renderRows();
                            }
                        }
                    });
                }
            );
    }

    /**
     * Cheap upper temporal bound:
     * set upper bound to one hour in the future
     * @private
     */
    private upto() {
        const d = new Date();
        d.setHours(d.getHours() + 1);
        return d.valueOf();
    }

}
