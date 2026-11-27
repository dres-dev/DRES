import { Component, Input } from '@angular/core';
import { ApiVerdictCounts } from '../../../openapi';

/** Compact display of answer-set counts grouped by verdict. */
@Component({
  selector: 'app-verdict-counts',
  template: `
    <span class="verdicts" *ngIf="counts; else none">
      <span class="v correct" matTooltip="Correct submissions"><mat-icon inline>check_circle</mat-icon>{{ counts.correct }}</span>
      <span class="v wrong" matTooltip="Wrong submissions"><mat-icon inline>cancel</mat-icon>{{ counts.wrong }}</span>
      <span class="v open" [class.pending]="counts.indeterminate > 0" matTooltip="Submissions not judged yet">
        <mat-icon inline>hourglass_top</mat-icon>{{ counts.indeterminate }}
      </span>
      <span class="v undecidable" *ngIf="counts.undecidable > 0" matTooltip="Submissions judged as undecidable">
        <mat-icon inline>help</mat-icon>{{ counts.undecidable }}
      </span>
    </span>
    <ng-template #none>–</ng-template>
  `,
  styles: [
    `
      .verdicts { display: inline-flex; gap: 0.75rem; white-space: nowrap; }
      .v { display: inline-flex; align-items: center; gap: 0.2rem; font-variant-numeric: tabular-nums; }
      .correct mat-icon { color: #4caf50; }
      .wrong mat-icon { color: #f44336; }
      .open mat-icon, .undecidable mat-icon { opacity: 0.6; }
      .open.pending { color: #ffa000; font-weight: 500; }
      .open.pending mat-icon { color: #ffa000; opacity: 1; }
    `,
  ],
  standalone: false,
})
export class VerdictCountsComponent {
  @Input() counts: ApiVerdictCounts;
}
