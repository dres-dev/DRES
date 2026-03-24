import { Component, Input, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { catchError, map, switchMap, filter, sampleTime, shareReplay } from 'rxjs/operators';
import { ApiEvaluationInfo, ApiEvaluationState, ApiMediaItem, EvaluationService } from '../../../../openapi';
import { AppConfig } from '../../app.config';
import { HttpClient } from '@angular/common/http';

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
  
  private serverTimeOffset = 0;
  private currentTaskId: string | null = null;
  private currentTaskStartTime: number = 0;

  constructor(
    private evaluationService: EvaluationService,
    private config: AppConfig,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    // Calculate the offset between server time and client time
    this.http.get(this.config.resolveApiUrl('/client/evaluation/list'), { observe: 'response', responseType: 'text' }).subscribe(res => {
      const serverDateStr = res.headers.get('Date');
      if (serverDateStr) {
        const serverTime = new Date(serverDateStr).getTime();
        this.serverTimeOffset = Date.now() - serverTime; 
      }
    });

    const submissions$ = this.state.pipe(
      sampleTime(2000),
      switchMap(st => this.evaluationService.getApiV2EvaluationByEvaluationIdSubmissionList(st.evaluationId).pipe(
        catchError(() => of([]))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    this.recentSubmissions$ = combineLatest([this.info, submissions$, this.state]).pipe(
      map(([info, submissions, state]) => {
        if (!info || !info.teams || !submissions || !state) return [];

        const isRunning = state.taskStatus === 'RUNNING';

        if (isRunning) {
          if (this.currentTaskId !== state.taskId) {
            this.currentTaskId = state.taskId;
            const syncedServerTime = Date.now() - this.serverTimeOffset;
            this.currentTaskStartTime = syncedServerTime - (state.timeElapsed * 1000);
          }
        } else {
          this.currentTaskId = null;
        }

        const teamMap = new Map<string, string>();
        info.teams.forEach(t => teamMap.set(t.id, t.name));

        const feed: any[] = [];

        submissions.forEach(sub => {
          const teamName = teamMap.get(sub.teamId) || 'Unknown';
          
          sub.answers.forEach((ans, index) => {
            const firstAns = ans.answers && ans.answers.length > 0 ? ans.answers[0] : null;
            
            let timeDisplay = '';

            // Branch for different time display logic based on whether the task is still running or not
            if (isRunning && this.currentTaskStartTime > 0) {
              // If running, display relative time since task start
              const relativeMs = Math.max(0, sub.timestamp - this.currentTaskStartTime);
              const totalSeconds = Math.floor(relativeMs / 1000);
              const minutes = Math.floor(totalSeconds / 60);
              const seconds = (totalSeconds % 60).toString().padStart(2, '0');
              timeDisplay = `${minutes}:${seconds}`;
            } else {
              // If ended, show absolute time
              const d = new Date(sub.timestamp);
              const hh = d.getHours().toString().padStart(2, '0');
              const mm = d.getMinutes().toString().padStart(2, '0');
              const ss = d.getSeconds().toString().padStart(2, '0');
              timeDisplay = `${hh}:${mm}:${ss}`;
            }
            
            feed.push({
              uniqueId: `${sub.submissionId}-${index}`,
              teamName: teamName,
              status: ans.status,
              type: firstAns?.type,
              previewUrl: this.previewOfItem(firstAns?.item, firstAns?.start),
              previewText: firstAns?.text,
              relativeTime: timeDisplay 
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
    return sub.uniqueId;
  }
}