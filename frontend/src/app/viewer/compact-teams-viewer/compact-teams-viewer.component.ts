import { Component, Input, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Observable, combineLatest, EMPTY, of } from 'rxjs';
import { catchError, map, switchMap, shareReplay, retry, startWith } from 'rxjs/operators';
import { ApiEvaluationInfo, ApiEvaluationState, EvaluationScoresService, EvaluationService } from '../../../../openapi';
import { CommonModule } from '@angular/common';
import { MatTooltipModule } from '@angular/material/tooltip';
import { IWsServerMessage } from '../../model/ws/ws-server-message.interface';
import { VIEWER_SCORE_UPDATE_TYPES, VIEWER_SUBMISSION_UPDATE_TYPES, viewerRefreshes } from '../viewer-refresh';

@Component({
  selector: 'app-compact-teams-viewer',
  templateUrl: './compact-teams-viewer.component.html',
  styleUrls: ['./compact-teams-viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatTooltipModule],
})
export class CompactTeamsViewerComponent implements OnInit {
  @Input() info: Observable<ApiEvaluationInfo>;
  @Input() state: Observable<ApiEvaluationState>;
  @Input() taskEnded: Observable<ApiEvaluationState>;
  @Input() messages: Observable<IWsServerMessage> = EMPTY;
  @Input() reconnected: Observable<void> = EMPTY;

  teamsData$: Observable<any[]>;

  constructor(
    private evaluationService: EvaluationService,
    private scoresService: EvaluationScoresService
  ) {}

  ngOnInit(): void {
    const submissions$ = viewerRefreshes(
      this.state,
      this.messages,
      this.reconnected,
      VIEWER_SUBMISSION_UPDATE_TYPES
    ).pipe(
      switchMap((evaluationId) =>
        this.evaluationService.getApiV2EvaluationByEvaluationIdSubmissionList(evaluationId).pipe(catchError(() => of([])))
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    const scores$ = viewerRefreshes(this.state, this.messages, this.reconnected, VIEWER_SCORE_UPDATE_TYPES).pipe(
      switchMap((evaluationId) =>
        this.scoresService.getApiV2ScoreEvaluationByEvaluationIdCurrent(evaluationId).pipe(
          retry(3),
          catchError(() => of(null))
        )
      ),
      map((sc) => {
        const scoreMap = new Map<string, number>();
        if (sc && sc.scores) {
          sc.scores.forEach((v) => scoreMap.set(v.teamId, v.score));
        }
        return scoreMap;
      }),
      startWith(new Map<string, number>()),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    // Combine everything into a single array for the UI
    this.teamsData$ = combineLatest([this.info, scores$, submissions$]).pipe(
      map(([info, scoreMap, submissions]) => {
        if (!info || !info.teams) return [];

        const teamsWithScores = info.teams.map((team) => {
          const teamSubmissions = submissions.filter((s) => s.teamId === team.id).flatMap((s) => s.answers);

          return {
            id: team.id,
            name: team.name,
            score: scoreMap.get(team.id) || 0,
            correct: teamSubmissions.filter((a) => a.status === 'CORRECT').length,
            wrong: teamSubmissions.filter((a) => a.status === 'WRONG').length,
            indeterminate: teamSubmissions.filter((a) => a.status === 'INDETERMINATE').length,
            rank: 0, // to be filled
          };
        });

        // Sort a copy by score to determine true ranks
        const sortedByScore = [...teamsWithScores].sort((a, b) => b.score - a.score);
        sortedByScore.forEach((team, index) => {
          const original = teamsWithScores.find((t) => t.id === team.id);
          if (original) {
            original.rank = index + 1;
          }
        });

        return teamsWithScores;
      })
    );
  }

}
