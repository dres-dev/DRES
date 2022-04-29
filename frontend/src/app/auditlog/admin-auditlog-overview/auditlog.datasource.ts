import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { AuditService, RestAuditLogEntry } from '../../../../openapi';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, first } from 'rxjs/operators';

/**
 * {@link DataSource} implementation of {@link RestAuditLogEntry}.
 */
export class AuditlogDatasource implements DataSource<RestAuditLogEntry> {
  /** {@link BehaviorSubject} used to publish {@link RestAuditLogEntry} array. */
  private logsSubject = new BehaviorSubject<RestAuditLogEntry[]>([]);
  private loadingSubject = new BehaviorSubject<boolean>(false);

  constructor(private logService: AuditService) {}

  /**
   * Connects this {@link AuditlogDatasource}.
   *
   * @param collectionViewer The collection viewer
   */
  connect(collectionViewer: CollectionViewer): Observable<RestAuditLogEntry[]> {
    return this.logsSubject.asObservable();
  }

  /**
   * Disconnects this {@link AuditlogDatasource}.
   *
   * @param collectionViewer the collection viewer
   */
  disconnect(collectionViewer: CollectionViewer): void {
    this.logsSubject.complete();
    this.loadingSubject.complete();
  }

  /**
   *
   * @param pageIndex the page index to request audit logs for
   * @param pageSize the page size to limit the number of logs
   */
  refresh(pageIndex = 0, pageSize = 100) {
    this.loadingSubject.next(true);
    this.logService
      .getApiV1AuditLogListLimitWithLimitWithPage(pageSize, pageIndex)
      .pipe(
        first(),
        catchError(() => of([])),
        finalize(() => this.loadingSubject.next(false))
      )
      .subscribe((logs) => this.logsSubject.next(logs));
  }
}
