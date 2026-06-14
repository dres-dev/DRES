import { Component, Input, OnInit } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { catchError, map, switchMap, filter } from 'rxjs/operators';
import { ApiEvaluationInfo, ApiEvaluationState, EvaluationScoresService } from '../../../../openapi';
import { CommonModule } from '@angular/common';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-compact-score-graph',
  templateUrl: './compact-score-graph.component.html',
  styleUrls: ['./compact-score-graph.component.scss'],
  imports: [CommonModule, MatTooltipModule],
})
export class CompactScoreGraphComponent implements OnInit {
  @Input() info: Observable<ApiEvaluationInfo>;
  @Input() state: Observable<ApiEvaluationState>;

  graphData$: Observable<any>;

  colorPalette = [
    '#9b59b6',
    '#3498db',
    '#e67e22',
    '#2ecc71',
    '#e74c3c',
    '#1abc9c',
    '#f1c40f',
    '#34495e',
    '#ff9ff3',
    '#feca57',
    '#ff6b6b',
    '#48dbfb',
    '#1dd1a1',
    '#5f27cd',
    '#c8d6e5',
    '#22a6b3',
    '#badc58',
    '#eb4d4b',
    '#686de0',
    '#30336b',
  ];

  constructor(private scoreService: EvaluationScoresService) {}

  ngOnInit(): void {
    const rawScores$ = this.state.pipe(
      filter((s) => !!s && !!s.evaluationId),
      switchMap((s) => this.scoreService.getApiV2ScoreEvaluationByEvaluationId(s.evaluationId).pipe(catchError(() => of([]))))
    );

    this.graphData$ = combineLatest([this.info, rawScores$]).pipe(
      map(([info, scores]) => {
        if (!info || !info.teams || !scores) return null;

        const validGroups = scores.filter((s) => s.name !== 'sum' && s.name !== 'average');
        const legend = validGroups.map((g, i) => ({
          name: g.name,
          color: this.colorPalette[i % this.colorPalette.length],
        }));

        let globalMaxScore = 0;

        const teamsData = info.teams.map((team) => {
          let totalScore = 0;
          const segments: any[] = [];

          validGroups.forEach((group, index) => {
            const teamScoreObj = group.scores?.find((ss) => ss.teamId === team.id);
            const value = teamScoreObj ? Math.round(teamScoreObj.score) : 0;

            totalScore += value;
            segments.push({
              name: group.name,
              value: value,
              color: this.colorPalette[index % this.colorPalette.length],
            });
          });

          if (totalScore > globalMaxScore) globalMaxScore = totalScore;

          return { name: team.name, total: totalScore, segments };
        });

        teamsData.sort((a, b) => b.total - a.total);
        const chartScaleMax = Math.max(globalMaxScore, 1000);

        teamsData.forEach((team) => {
          team.segments.forEach((seg) => {
            seg.widthInPercent = (seg.value / chartScaleMax) * 100;
          });
        });

        return { teams: teamsData, legend: legend };
      })
    );
  }
}
