import { Component, Input, OnInit } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { catchError, map, switchMap, filter } from 'rxjs/operators';
import { ApiEvaluationInfo, ApiEvaluationState, EvaluationScoresService } from '../../../../openapi';
import { CommonModule } from '@angular/common';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-leaderboard-viewer',
  templateUrl: './leaderboard-viewer.component.html',
  styleUrls: ['./leaderboard-viewer.component.scss'],
  imports: [CommonModule, MatTooltipModule]
})
export class LeaderboardViewerComponent implements OnInit {
  @Input() info: Observable<ApiEvaluationInfo>;
  @Input() state: Observable<ApiEvaluationState>;

  playerData$: Observable<any[]>;

  constructor(private scoreService: EvaluationScoresService) {}

  ngOnInit(): void {
    const rawScores$ = this.state.pipe(
      filter(s => !!s && !!s.evaluationId),
      switchMap(s => this.scoreService.getApiV2ScoreEvaluationByEvaluationId(s.evaluationId).pipe(
        catchError(() => of([]))
      ))
    );

    this.playerData$ = combineLatest([this.info, rawScores$]).pipe(
      map(([info, scores]) => {
        if (!info || !info.teams || !scores || scores.length === 0) return [];

        const players = info.teams.map(team => {
          let totalScore = 0;
          // Tally up the total score from all groups, ignoring the 'sum' and 'average' meta-fields
          scores.filter(s => s.name !== 'sum' && s.name !== 'average').forEach(group => {
            const teamScoreObj = group.scores?.find(ss => ss.teamId === team.id);
            if (teamScoreObj) totalScore += Math.round(teamScoreObj.score);
          });

          return { userName: team.name, totalScore: totalScore };
        });

        // Return ALL players, sorted highest to lowest
        return players.sort((a, b) => b.totalScore - a.totalScore);
      })
    );
  }
}