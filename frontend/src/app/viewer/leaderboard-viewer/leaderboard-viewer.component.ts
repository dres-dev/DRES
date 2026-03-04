import { Component, Input, OnInit } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { catchError, map, switchMap, filter } from 'rxjs/operators';
import { ApiEvaluationInfo, ApiEvaluationState, EvaluationScoresService } from '../../../../openapi';

@Component({
  selector: 'app-leaderboard-viewer',
  templateUrl: './leaderboard-viewer.component.html',
  styleUrls: ['./leaderboard-viewer.component.scss']
})
export class LeaderboardViewerComponent implements OnInit {
  @Input() info: Observable<ApiEvaluationInfo>;
  @Input() state: Observable<ApiEvaluationState>;

  playerData$: Observable<any[]>;
  taskGroupNames$: Observable<string[]>;

  constructor(private scoreService: EvaluationScoresService) {}

  ngOnInit(): void {
    // 1. Fetch the raw scores whenever the state updates
    const rawScores$ = this.state.pipe(
      filter(s => !!s && !!s.evaluationId), // Make sure we have an ID
      switchMap(s => this.scoreService.getApiV2ScoreEvaluationByEvaluationId(s.evaluationId).pipe(
        catchError(err => {
          console.error('Error retrieving scores:', err);
          return of([]); // Return empty array on error so UI doesn't crash
        })
      ))
    );

    // 2. Extract just the Task Group Names for the table headers
    this.taskGroupNames$ = rawScores$.pipe(
      map(scores => {
        // Filter out the 'sum' or 'average' meta-scores, we just want task groups
        return scores
          .map(s => s.name)
          .filter(name => name !== 'sum' && name !== 'average');
      })
    );

    // 3. Combine Teams and Scores to build the final Player Array
    this.playerData$ = combineLatest([this.info, rawScores$, this.taskGroupNames$]).pipe(
      map(([info, scores, groupNames]) => {
        if (!info || !info.teams || !scores || scores.length === 0) {
          return [];
        }

        // Create a player object for each team
        const players = info.teams.map(team => {
          const player: any = {
            userName: team.name,
            totalScore: 0,
            taskGroupScores: {}
          };

          // Loop through every task group and assign the score for this specific team
          groupNames.forEach(groupName => {
            const groupData = scores.find(s => s.name === groupName);
            const teamScoreObj = groupData?.scores?.find(ss => ss.teamId === team.id);
            
            const scoreValue = teamScoreObj ? Math.round(teamScoreObj.score) : 0;
            player.taskGroupScores[groupName] = scoreValue;
            player.totalScore += scoreValue; // Tally up the total
          });

          return player;
        });

        // Sort by total score, highest first, and grab the top 10
        return players
          .sort((a, b) => b.totalScore - a.totalScore)
          .slice(0, 10);
      })
    );
  }
}