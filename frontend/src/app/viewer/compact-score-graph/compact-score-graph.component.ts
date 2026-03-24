import { Component, Input, OnInit } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { catchError, map, switchMap, filter } from 'rxjs/operators';
import { ApiEvaluationInfo, ApiEvaluationState, EvaluationScoresService } from '../../../../openapi';

@Component({
  selector: 'app-compact-score-graph',
  templateUrl: './compact-score-graph.component.html',
  styleUrls: ['./compact-score-graph.component.scss']
})
export class CompactScoreGraphComponent implements OnInit {
  @Input() info: Observable<ApiEvaluationInfo>;
  @Input() state: Observable<ApiEvaluationState>;

  graphData$: Observable<any>;
  
  // A sleek, colorblind-friendly palette for the different task groups
  colorPalette = ['#9b59b6', '#3498db', '#e67e22', '#2ecc71', '#e74c3c', '#1abc9c'];

  constructor(private scoreService: EvaluationScoresService) {}

  ngOnInit(): void {
    const rawScores$ = this.state.pipe(
      filter(s => !!s && !!s.evaluationId),
      switchMap(s => this.scoreService.getApiV2ScoreEvaluationByEvaluationId(s.evaluationId).pipe(
        catchError(() => of([]))
      ))
    );

    this.graphData$ = combineLatest([this.info, rawScores$]).pipe(
      map(([info, scores]) => {
        if (!info || !info.teams || !scores) return null;

        // 1. Extract valid task groups (ignoring meta-scores)
        const validGroups = scores.filter(s => s.name !== 'sum' && s.name !== 'average');
        const legend = validGroups.map((g, i) => ({
          name: g.name,
          color: this.colorPalette[i % this.colorPalette.length]
        }));

        let globalMaxScore = 0;

        // 2. Map teams and calculate segments
        const teamsData = info.teams.map(team => {
          let totalScore = 0;
          const segments: any[] = [];

          validGroups.forEach((group, index) => {
            const teamScoreObj = group.scores?.find(ss => ss.teamId === team.id);
            const value = teamScoreObj ? Math.round(teamScoreObj.score) : 0;
            
            totalScore += value;
            segments.push({
              name: group.name,
              value: value,
              color: this.colorPalette[index % this.colorPalette.length]
            });
          });

          if (totalScore > globalMaxScore) globalMaxScore = totalScore;

          return { name: team.name, total: totalScore, segments };
        });

        // 3. Sort descending by total score
        teamsData.sort((a, b) => b.total - a.total);

        // 4. Calculate the width percentages. (Scale to the highest score, or minimum 1000)
        const chartScaleMax = Math.max(globalMaxScore, 1000);

        teamsData.forEach(team => {
          team.segments.forEach(seg => {
            seg.widthPct = (seg.value / chartScaleMax) * 100;
          });
        });

        return { teams: teamsData, legend: legend };
      })
    );
  }
}