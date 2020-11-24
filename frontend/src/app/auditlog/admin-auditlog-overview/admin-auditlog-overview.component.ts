import {AfterViewInit, Component, OnDestroy, ViewChild} from '@angular/core';
import {AuditService, RestAuditLogEntry} from '../../../../openapi';
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
    displayCols = ['time', 'api', 'type', 'id']; // TODO clever way to dynamically list things
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
