import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ApiCollectionIssue } from '../../../../openapi';

export interface CollectionReportDialogData {
  title: string;
  /** Summary lines shown above the issue list. */
  summary: string[];
  issues: ApiCollectionIssue[];
  /** Total number of issues; may be larger than the number of listed ones. */
  total: number;
}

/** Shows the result of a collection check or bulk deletion. */
@Component({
  selector: 'app-collection-report-dialog',
  template: `
    <h1 mat-dialog-title>{{ data.title }}</h1>
    <div mat-dialog-content class="maintenance-dialog">
      <p *ngFor="let line of data.summary">{{ line }}</p>
      <app-collection-issue-list *ngIf="data.issues?.length" [issues]="data.issues" [total]="data.total"></app-collection-issue-list>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Close</button>
    </div>
  `,
  styleUrls: ['./collection-maintenance.scss'],
  standalone: false,
})
export class CollectionReportDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: CollectionReportDialogData) {}
}
