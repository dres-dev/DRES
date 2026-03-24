import { Component, Input, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { catchError, map, switchMap, filter, sampleTime, shareReplay } from 'rxjs/operators';
import { ApiEvaluationInfo, ApiEvaluationState, ApiMediaItem, EvaluationService } from '../../../../openapi';
import { AppConfig } from '../../app.config';

@Component({
  selector: 'app-recent-submissions',
  templateUrl: './recent-submissions.component.html',
  styleUrls: ['./recent-submissions.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecentSubmissionsComponent implements OnInit {
  @Input() info: Observable<ApiEvaluationInfo>;
  @Input() state: Observable<ApiEvaluationState>;

  recentSubmissions$: Observable<any[]>;

  constructor(
    private evaluationService: EvaluationService,
    private config: AppConfig
  ) {}

  ngOnInit(): void {
    const submissions$ = this.state.pipe(
      sampleTime(2000),
      switchMap(st => this.evaluationService.getApiV2EvaluationByEvaluationIdSubmissionList(st.evaluationId).pipe(
        catchError(() => of([]))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    this.recentSubmissions$ = combineLatest([this.info, submissions$]).pipe(
      map(([info, submissions]) => {
        if (!info || !info.teams || !submissions) return [];

        // Create a lookup map for fast team name resolution
        const teamMap = new Map<string, string>();
        info.teams.forEach(t => teamMap.set(t.id, t.name));

        const feed = [];

        submissions.forEach(sub => {
          const teamName = teamMap.get(sub.teamId) || 'Unknown';
          
          sub.answers.forEach((ans, index) => {
            const firstAns = ans.answers && ans.answers.length > 0 ? ans.answers[0] : null;
            
            feed.push({
              uniqueId: `${sub.submissionId}-${index}`,
              teamName: teamName,
              status: ans.status,
              type: firstAns?.type,
              previewUrl: this.previewOfItem(firstAns?.item, firstAns?.start),
              previewText: firstAns?.text
            });
          });
        });

        return feed.reverse().slice(0, 30);
      })
    );
  }

  public previewOfItem(item: ApiMediaItem, start: number): string | null {
    if (!item) return null;
    return this.config.resolveApiUrl(`/preview/${item.mediaItemId}/${start == null ? 0 : start}`);
  }

  public trackByUniqueId(index: number, sub: any): string {
    return sub.uniqueId; // Prevents Angular from constantly reloading images
  }
}