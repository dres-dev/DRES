import {AfterViewInit, Component, OnDestroy, ViewChild} from '@angular/core';
import {
    AuditService,
    RestAuditLogEntry,
    RestCompetitionEndAuditLogEntry,
    RestJudgementAuditLogEntry,
    RestLoginAuditLogEntry,
    RestLogoutAuditLogEntry,
    RestSubmissionAuditLogEntry,
    RestTaskEndAuditLogEntry,
    RestTaskModifiedAuditLogEntry,
    RestTaskStartAuditLogEntry
} from '../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Subscription, timer} from 'rxjs';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {switchMap} from 'rxjs/operators';
import {AuditlogDatasource} from './auditlog.datasource';

@Component({
    selector: 'app-admin-auditlog-overview',
    templateUrl: './admin-auditlog-overview.component.html',
    styleUrls: ['./admin-auditlog-overview.component.scss']
})
export class AdminAuditlogOverviewComponent implements AfterViewInit, OnDestroy {

    displayCols = ['time', 'api', 'type', 'details', 'id']; // TODO clever way to dynamically list things
    pollingFrequency = 5000; // every second

    /** Material Table UI reference. */
    @ViewChild('table', {static: true}) table;

    /** Material Table UI element for sorting. */
    @ViewChild(MatSort) sort: MatSort;

    /** Material Table UI element for pagination. */
    @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;

    /** Data source for Material table */
    public dataSource: AuditlogDatasource;

    /** Number of audit log items. */
    public length = 0;

    /** Subscription used for polling audit logs. */
    private pollingSub: Subscription;

    /** Subscription used for pagination. */
    private paginationSub: Subscription;

    constructor(private snackBar: MatSnackBar, private logService: AuditService) {
        this.dataSource = new AuditlogDatasource(logService);
    }

    /**
     * Initialize subscription for loading audit logs.
     *
     * IMPORTANT: Unsubscribe OnDestroy!
     */
    public ngAfterViewInit(): void {
        /* Initialize subscription for loading audit logs. */
        this.pollingSub = timer(0, this.pollingFrequency).pipe(
            switchMap(s => this.logService.getApiV1AuditInfo()),
        ).subscribe(i => {
            this.length = i.size;
            if (this.paginator.pageIndex === 0) { /* Only the first page needs refreshing because logs are ordered chronologically. */
                this.dataSource.refresh(this.paginator.pageIndex, this.paginator.pageSize);
            }
        });

        /* Initialize subscription for pagination. */
        this.paginationSub = this.paginator.page.subscribe(p => {
            this.dataSource.refresh(this.paginator.pageIndex, this.paginator.pageSize);
        });
    }

    /**
     * House keeping; clean up subscriptions.
     */
    public ngOnDestroy(): void {
        this.pollingSub.unsubscribe();
        this.pollingSub = null;

        this.paginationSub.unsubscribe();
        this.paginationSub = null;
    }

    resolveAuditLogEntryById(_: number, item: RestAuditLogEntry){
        return item.id;
    }

    public detailsOf(log: RestAuditLogEntry): string {
        switch (log.type) {
            case 'COMPETITION_START':
                const cs = (log as unknown) as RestCompetitionEndAuditLogEntry;
                return `Competition ${cs.competition} has been started by user ${cs.user}`;
            case 'COMPETITION_END':
                const ce = (log as unknown) as RestCompetitionEndAuditLogEntry;
                return `Competition ${ce.competition} has been ended by user ${ce.user}`;
            case 'TASK_START':
                const ts = (log as unknown) as RestTaskStartAuditLogEntry;
                return `Task ${ts.taskName} in competition ${ts.competition} has been started by user ${ts.user}`;
            case 'TASK_MODIFIED':
                const tm = (log as unknown) as RestTaskModifiedAuditLogEntry;
                return `Task ${tm.taskName} in competition ${tm.competition} has been modified by user ${tm.user}: ${tm.modification}`;
            case 'TASK_END':
                const te = (log as unknown) as RestTaskEndAuditLogEntry;
                return `Task ${te.taskName} in competition ${te.competition} has been ended by user ${te.user}`;
            case 'SUBMISSION':
                const subm = (log as unknown) as RestSubmissionAuditLogEntry;
                return `For ${subm.taskName}, a submission ${JSON.stringify(subm.submission)} was made in competition ${subm.competition}
                 by user ${subm.user} from ${subm.address}`.replace('\n', '');
            case 'JUDGEMENT':
                const judgement = (log as unknown) as RestJudgementAuditLogEntry;
                return `Judge ${judgement.user ? judgement.user : ''} published verdict ${judgement.verdict} for token ${judgement.token}
                 based on validator ${judgement.validator} in competition ${judgement.competition}`.replace('\n', '');
            case 'LOGIN':
                const login = (log as unknown) as RestLoginAuditLogEntry;
                return `${login.user} has logged in using ${login.session}`;
            case 'LOGOUT':
                const logout = (log as unknown) as RestLogoutAuditLogEntry;
                return `${logout.session} was logged out`;
            default:
                return JSON.stringify(log);
        }
    }
}
