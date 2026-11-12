import { Component, Input } from '@angular/core';
import { ApiCollectionIssue } from '../../../../openapi';

/** Compact list of files or items with a problem description. */
@Component({
  selector: 'app-collection-issue-list',
  template: `
    <div class="issue-list">
      <div class="issue" *ngFor="let issue of issues">
        <code class="location">{{ issue.location }}</code>
        <span class="problem">{{ issue.problem }}</span>
      </div>
    </div>
    <p *ngIf="total > issues.length" class="muted">… and {{ total - issues.length }} more.</p>
  `,
  styleUrls: ['./collection-maintenance.scss'],
  standalone: false,
})
export class CollectionIssueListComponent {
  @Input() issues: ApiCollectionIssue[] = [];
  @Input() total = 0;
}
