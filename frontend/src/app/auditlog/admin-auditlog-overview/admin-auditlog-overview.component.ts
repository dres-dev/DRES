import { AfterViewInit, Component, OnDestroy, ViewChild } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, timer } from 'rxjs';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { switchMap } from 'rxjs/operators';
import { AuditlogDatasource } from './auditlog.datasource';
import {ApiAuditLogEntry, AuditLogInfo, AuditService} from '../../../../openapi';

@Component({
  selector: 'app-admin-auditlog-overview',
  templateUrl: './admin-auditlog-overview.component.html',
  styleUrls: ['./admin-auditlog-overview.component.scss'],
})
export class AdminAuditlogOverviewComponent implements AfterViewInit, OnDestroy {
  displayCols = ['time', 'api', 'type', 'details', 'id']; // TODO clever way to dynamically list things
  pollingFrequency = 5000; // every second

  /** Material Table UI reference. */
  @ViewChild('table', { static: true }) table;

  /** Material Table UI element for sorting. */
  @ViewChild(MatSort) sort: MatSort;

  /** Material Table UI element for pagination. */
  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;

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
    this.pollingSub = timer(0, this.pollingFrequency)
      .pipe(switchMap((s) => this.logService
          .getApiV2AuditInfo()))
      .subscribe((i: AuditLogInfo) => {
        this.length = i.size;
        if (this.paginator.pageIndex === 0) {
          /* Only the first page needs refreshing because logs are ordered chronologically. */
          this.dataSource.refresh(this.paginator.pageIndex, this.paginator.pageSize);
        }
      });

    /* Initialize subscription for pagination. */
    this.paginationSub = this.paginator.page.subscribe((p) => {
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

  resolveAuditLogEntryById(_: number, item: ApiAuditLogEntry) {
    return item.id;
  }

  public detailsOf(log: ApiAuditLogEntry): string {
    // TODO new design probably asks for the description to be printed -- alone?
    switch (log.type) {
      case 'PREPARE_JUDGEMENT':
        return `Judgement preparation in competition ${log?.competitionId} | ${log?.description}.`;
      case 'SUBMISSION_VALIDATION':
        return `Submission validation in competition ${log?.competitionId} for submission ${log?.submissionId} | ${log?.description}.`;
      case 'SUBMISSION_STATUS_OVERWRITE':
        return `Submission status overwrite in competition ${log?.competitionId} for submission ${log.submissionId} | ${log?.description}.`;
      case 'COMPETITION_START':
        return `Competition ${log.competitionId} has been started by user ${log.userId} | ${log?.description}.`;
      case 'COMPETITION_END':
        return `Competition ${log.competitionId} has been ended by user ${log.userId} | ${log?.description}.`;
      case 'TASK_START':
        // return `Task ${log.taskId} in competition ${log.competitionId} has been started by user ${log.userId}`;
        return `Task *** in competition ${log.competitionId} has been started by user ${log.userId} | ${log?.description}.`;
      case 'TASK_MODIFIED':
        // return `Task ${log.taskId} in competition ${log.competitionId} has been modified by user ${log.userId}: ${log.modification}`;
        return `Task *** in competition ${log.competitionId} has been modified by user ${log.userId}: *** | ${log?.description}.`;
      case 'TASK_END':
        return `Task *** in competition ${log.competitionId} has been ended by user ${log.userId} | ${log?.description}.`;
      case 'SUBMISSION':
        return `For task ***, a submission ${log.submissionId} was made in competition ${
          log.competitionId
        }
                 by user ${log.userId} from ${log.address} | ${log?.description}.`.replace('\n', '');
      case 'JUDGEMENT':
        return `Judge ${log.userId ? log.userId : ''} published a verdict  in competition ${log.competitionId}  | ${log?.description}.`.replace('\n', '');
      case 'LOGIN':
        return `${log.userId} has logged in using ${log.session}  | ${log?.description}.`;
      case 'LOGOUT':
        return `${log.session} was logged out | ${log?.description}.`;
      default:
        return JSON.stringify(log);
    }
  }
}
