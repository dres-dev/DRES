import { Component, Input, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { Observable, combineLatest, of, merge, BehaviorSubject, Subscription } from 'rxjs';
import { catchError, map, switchMap, filter, sampleTime, shareReplay, pairwise, retry, startWith } from 'rxjs/operators';
import { animate, keyframes, style, transition, trigger } from '@angular/animations';
import { ApiEvaluationInfo, ApiEvaluationState, ApiSubmission, EvaluationScoresService, EvaluationService } from '../../../../openapi';

@Component({
  selector: 'app-compact-teams-viewer',
  templateUrl: './compact-teams-viewer.component.html',
  styleUrls: ['./compact-teams-viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('highlight', [
      transition('* => correct', animate('1500ms', keyframes([
        style({ backgroundColor: '#2a4a3a', borderColor: '#386148', offset: 0 }),
        style({ backgroundColor: '#2a4a3a', borderColor: '#386148', offset: 0.8 }),
        style({ backgroundColor: '#2c2c2e', borderColor: '#333', offset: 1 })
      ]))),
      transition('* => wrong', animate('1500ms', keyframes([
        style({ backgroundColor: '#4a2a2a', borderColor: '#613838', offset: 0 }),
        style({ backgroundColor: '#4a2a2a', borderColor: '#613838', offset: 0.8 }),
        style({ backgroundColor: '#2c2c2e', borderColor: '#333', offset: 1 })
      ])))
    ])
  ]
})
export class CompactTeamsViewerComponent implements OnInit, OnDestroy {
  @Input() info: Observable<ApiEvaluationInfo>;
  @Input() state: Observable<ApiEvaluationState>;
  @Input() taskEnded: Observable<ApiEvaluationState>;

  teamsData$: Observable<any[]>;
  highlight$: Observable<Map<string, string>>;
  resetHighlight: BehaviorSubject<void> = new BehaviorSubject(null);
  
  private intervalId: any;

  constructor(
    private evaluationService: EvaluationService,
    private scoresService: EvaluationScoresService,
    private ref: ChangeDetectorRef
  ) {
    this.ref.detach();
    this.intervalId = setInterval(() => this.ref.detectChanges(), 500);
  }

  ngOnInit(): void {
    // Fetch submissions every 2 seconds
    const submissions$ = this.state.pipe(
      sampleTime(2000),
      switchMap(st => this.evaluationService.getApiV2EvaluationByEvaluationIdSubmissionList(st.evaluationId).pipe(
        catchError(() => of([]))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    // Fetch scores
    const scores$ = this.state.pipe(
      switchMap(st => this.scoresService.getApiV2ScoreEvaluationByEvaluationIdCurrent(st.evaluationId).pipe(
        retry(3),
        catchError(() => of(null))
      )),
      map(sc => {
        const scoreMap = new Map<string, number>();
        if (sc && sc.scores) {
          sc.scores.forEach(v => scoreMap.set(v.teamId, v.score));
        }
        return scoreMap;
      }),
      startWith(new Map<string, number>()),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    // Animation Highlights
    const submissionDelta$ = combineLatest([submissions$, this.info]).pipe(
      map(([submissions, info]) => {
        const perTeam = new Map<string, ApiSubmission[]>();
        info.teams.forEach(t => perTeam.set(t.id, submissions.filter(s => s.teamId === t.id)));
        return perTeam;
      }),
      pairwise(),
      map(([s1, s2]) => {
        const delta = new Map<string, any>();
        for (const [key, value] of s1) {
          const newCorrect = s2.get(key)?.flatMap(s => s.answers).filter(a => a.status === 'CORRECT').length || 0;
          const oldCorrect = value.flatMap(s => s.answers).filter(a => a.status === 'CORRECT').length || 0;
          
          const newWrong = s2.get(key)?.flatMap(s => s.answers).filter(a => a.status === 'WRONG').length || 0;
          const oldWrong = value.flatMap(s => s.answers).filter(a => a.status === 'WRONG').length || 0;

          delta.set(key, { correct: Math.max(newCorrect - oldCorrect, 0), wrong: Math.max(newWrong - oldWrong, 0) });
        }
        return delta;
      })
    );

    this.highlight$ = merge(
      submissionDelta$.pipe(
        map(delta => {
          const highlight = new Map<string, string>();
          for (const [key, value] of delta) {
            if (value.correct > value.wrong) highlight.set(key, 'correct');
            else if (value.wrong > value.correct) highlight.set(key, 'wrong');
            else highlight.set(key, 'nohighlight');
          }
          return highlight;
        })
      ),
      this.resetHighlight.pipe(
        switchMap(() => this.info),
        map(info => {
          const highlight = new Map<string, string>();
          info.teams.forEach(t => highlight.set(t.id, 'nohighlight'));
          return highlight;
        })
      )
    ).pipe(startWith(new Map<string, string>()), shareReplay({ bufferSize: 1, refCount: true }));

    // Combine everything into a single array for the UI
    this.teamsData$ = combineLatest([this.info, scores$, submissions$]).pipe(
      map(([info, scoreMap, submissions]) => {
        if (!info || !info.teams) return [];

        // base array with scores and submission counts
        const teamsWithScores = info.teams.map(team => {
          const teamSubmissions = submissions.filter(s => s.teamId === team.id).flatMap(s => s.answers);
          
          return {
            id: team.id,
            name: team.name,
            score: scoreMap.get(team.id) || 0,
            correct: teamSubmissions.filter(a => a.status === 'CORRECT').length,
            wrong: teamSubmissions.filter(a => a.status === 'WRONG').length,
            indeterminate: teamSubmissions.filter(a => a.status === 'INDETERMINATE').length,
            rank: 0 // To be filled
          };
        });

        // Sort a copy by score to determine the true ranks
        const sortedByScore = [...teamsWithScores].sort((a, b) => b.score - a.score);
        sortedByScore.forEach((team, index) => {
          const original = teamsWithScores.find(t => t.id === team.id);
          if (original) {
            original.rank = index + 1;
          }
        });

        return teamsWithScores;
      })
    );
  }

  ngOnDestroy(): void {
    clearInterval(this.intervalId);
  }
}