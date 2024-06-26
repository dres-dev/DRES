import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Input,
    OnDestroy,
    ViewChild,
} from '@angular/core';
import {BehaviorSubject, combineLatest, merge, mergeMap, Observable, of, Subscription} from 'rxjs';
import {catchError, filter, map, pairwise, retry, sampleTime, shareReplay, switchMap, withLatestFrom,} from 'rxjs/operators';
import {AppConfig} from '../app.config';
import {animate, keyframes, style, transition, trigger} from '@angular/animations';
import {
  ApiAnswerType, ApiEvaluationInfo, ApiEvaluationState, ApiMediaItem, ApiScoreOverview,
  ApiSubmission, ApiTeam, ApiVerdictStatus, EvaluationScoresService, EvaluationService
} from "openapi";
import { HttpErrorResponse } from '@angular/common/http';

/**
 * Internal helper interface.
 */
interface SubmissionDelta {
  correct: number;
  wrong: number;
}

/**
 * Internal helper interface for submission previews.
 */
interface SubmissionPreview {
    submissionId: string
    answerIndex: number
    status: ApiVerdictStatus
    type: ApiAnswerType
    previewItem: ApiMediaItem
    previewText: string
    previewStart: number
    previewEnd: number
    previewImage: string
}


@Component({
  selector: 'app-teams-viewer',
  templateUrl: './teams-viewer.component.html',
  styleUrls: ['./teams-viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('highlight', [
      transition(
        'nohighlight => correct',
        animate(
          '1500ms',
          keyframes([
            style({ backgroundColor: 'initial', offset: 0 }),
            style({ backgroundColor: 'lightgreen', offset: 0.1 }),
            style({ backgroundColor: 'initial', offset: 1 }),
          ])
        )
      ),
      transition(
        'nohighlight => wrong',
        animate(
          '1500ms',
          keyframes([
            style({ backgroundColor: 'initial', offset: 0 }),
            style({ backgroundColor: 'tomato', offset: 0.1 }),
            style({ backgroundColor: 'initial', offset: 1 }),
          ])
        )
      ),
    ]),
  ],
})
export class TeamsViewerComponent implements AfterViewInit, OnDestroy {
  @Input() runId: Observable<string>;

  /** Observable for information about the current run. Usually queried once when the view is loaded. */
  @Input() info: Observable<ApiEvaluationInfo>;

  /** Observable for information about the current run's {@link ApiEvaluationState}. */
  @Input() state: Observable<ApiEvaluationState>;

  /** Observable that fires whenever a task ends. Emits the {@link ApiEvaluationState} that triggered the fire. */
  @Input() taskEnded: Observable<ApiEvaluationState>;

  /** Observable that tracks all the submissions. */
  submissions: Observable<ApiSubmission[]>;

  /** Observable that tracks all the submissions per team. */
  submissionsPerTeam: Observable<Map<string, ApiSubmission[]>>;

  /** Observable that tracks the current score per team. */
  scores: Observable<Map<string, number>>;

  /** Observable that tracks whether a highlight animation should be played for the given team. */
  highlight: Observable<Map<string, string>>;

  /** Behaviour subject used to reset highlight animation state. */
  resetHighlight: BehaviorSubject<void> = new BehaviorSubject(null);

  /** Reference to the audio elements played during countdown. */
  @ViewChild('audio_correct') correct: ElementRef<HTMLAudioElement>;
  @ViewChild('audio_wrong') wrong: ElementRef<HTMLAudioElement>;
  @ViewChild('audio_applause') applause: ElementRef<HTMLAudioElement>;
  @ViewChild('audio_trombone') trombone: ElementRef<HTMLAudioElement>;

  /** Internal subscription for playing sound effect of a task that has ended. */
  taskEndedSoundEffect: Subscription;

  lastTrackMap: Map<string, number> = new Map<string, number>();
  submissionTrackInterval: number = 60_000;

  constructor(
    private evaluationService: EvaluationService,
    private scoresService: EvaluationScoresService,
    private ref: ChangeDetectorRef,
    public config: AppConfig
  ) {
    this.ref.detach();
    setInterval(() => {
      this.ref.detectChanges();
    }, 500);
  }

    private playOnce(audio: HTMLAudioElement) {
        if (this.config.config.effects.mute) {
            return
        }
        audio
            .play()
            .catch((reason) => console.warn('Failed to play audio effects due to an error:', reason))
            .then(() => {});
    }

  ngAfterViewInit(): void {
    /* Create source observable; list of all submissions.  */
    this.submissions = this.state.pipe(
      sampleTime(2000), //only check once every two seconds
      switchMap((st) =>
        this.evaluationService.getApiV2EvaluationByEvaluationIdSubmissionList(st.evaluationId).pipe(
          catchError((err: HttpErrorResponse) => {
            if (err.status != 404) { //log anything but 404
              console.log(`[TeamsViewerComponent] Error while loading submissions: ${err?.message}.`);
            }
            return of(null);
          }),
          filter((sb) => sb != null) /* Filter null responses. */
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading of submission. */
    );

    /* Observable that tracks all the submissions per team. */
    this.submissionsPerTeam = combineLatest([this.submissions, this.info]).pipe(
      map(([submissions, info]) => {
        const submissionsPerTeam = new Map<string, ApiSubmission[]>();
        info.teams.forEach((t) => {
          submissionsPerTeam.set(t.id, submissions.filter((s) => s.teamId === t.id));
        });
        return submissionsPerTeam;
      }),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading of submission. */
    );

    /* Observable that tracks the current score per team. */
    this.scores = this.state.pipe(
      switchMap((st) =>
        this.scoresService.getApiV2ScoreEvaluationByEvaluationIdCurrent(st.evaluationId).pipe(
          retry(3),
          catchError((err: HttpErrorResponse) => {
            if (err.status != 404) { //log anything but 404
              console.log(`[TeamsViewerComponent] Error while loading scores: ${err?.message}.`);
            }
            return of(null);
          }),
          filter((sc) => sc != null) /* Filter null responses. */
        )
      ),
      map((sc: ApiScoreOverview) => {
        const scores = new Map<string, number>();
        sc.scores.forEach((v) => scores.set(v.teamId, v.score));
        return scores;
      }),
      shareReplay({ bufferSize: 1, refCount: true }) /* Cache last successful loading of score. */
    );

    /* Observable that calculates changes to the submission every 250ms (for sound effects playback). */
    const submissionDelta: Observable<Map<string, SubmissionDelta>> = this.submissionsPerTeam.pipe(
      pairwise(),
      map(([s1, s2]) => {
        const delta = new Map<string, SubmissionDelta>();
        for (const [key, value] of s1) {
          delta.set(key, {
            correct: Math.max(s2.get(key).flatMap(s => s.answers).filter((s) => s.status === 'CORRECT').length
                - value.flatMap(s => s.answers).filter((s) => s.status === 'CORRECT').length, 0),
            wrong: Math.max(s2.get(key).flatMap(s => s.answers).filter((s) => s.status === 'WRONG').length
                - value.flatMap(s => s.answers).filter((s) => s.status === 'WRONG').length, 0),
          } as SubmissionDelta);
        }
        return delta;
      })
    );

    /* Observable that indicates whether a certain team has new submissions. */
    this.highlight = merge(
      submissionDelta.pipe(
        map((delta) => {
          const highlight = new Map<string, string>();
          for (const [key, value] of delta) {
            if (value.correct > value.wrong) {
              highlight.set(key, 'correct');
              this.playOnce(this.correct.nativeElement);
            } else if (value.wrong > value.correct) {
              highlight.set(key, 'wrong');
              this.playOnce(this.wrong.nativeElement);
            } else {
              highlight.set(key, 'nohighlight');
            }
          }
          return highlight;
        })
      ),
      this.resetHighlight.pipe(
        mergeMap(() => this.info),
        map((info) => {
          const hightlight = new Map<string, string>();
          info.teams.forEach((t) => hightlight.set(t.id, 'nohighlight'));
          return hightlight;
        })
      )
    ).pipe(shareReplay({ bufferSize: 1, refCount: true })) /* Cache last successful loading of score. *///);

    /* Subscription for end of task (used to play sound effects). */
    this.taskEndedSoundEffect = this.taskEnded
      .pipe(
        withLatestFrom(this.submissions),
        map(([ended, submissions]) => {
          for (const s of submissions) {
              for (const a of s.answers) {
                  if (a.status === 'CORRECT') {
                      return true;
                  }
              }
          }
          return false;
        })
      )
      .subscribe((success) => {
          if (success) {
            this.playOnce(this.applause.nativeElement);
          } else {
            this.playOnce(this.trombone.nativeElement);
          }
      });
  }

  public ngOnDestroy(): void {
    this.taskEndedSoundEffect.unsubscribe(); /* IMPORTANT. */
    this.taskEndedSoundEffect = null;
  }

  /**
   * Generates a URL for the preview image of a submission.
   */
  public previewOfItem(item: ApiMediaItem, start: number): string {
    return this.config.resolveApiUrl(`/preview/${item.mediaItemId}/${start == null ? 0 : start}`)
  }

  /**
   * Generates a URL for the logo of the team.
   */
  public teamLogo(teamId: string): string {
    return this.config.resolveApiUrl(`/template/logo/${teamId}`);
  }

  /**
   * Sorts the given {@link ApiTeam}s based on the team name (lexicographically)
   */
  public orderTeamsByName = (team1: ApiTeam, team2: ApiTeam) => {
    return team1.name.localeCompare(team2.name)
  }

  /**
   * Returns an observable for the {@link ApiSubmission} for the given team.
   *
   * @param teamId The team's uid.
   */
  public submissionPreviews(teamId: string): Observable<SubmissionPreview[]> {
    return combineLatest([this.info, this.submissionsPerTeam]).pipe(
        map(([i, s]) => {
          if (s != null) {
              return s.get(teamId).flatMap(s => s.answers.map((a, i) => <SubmissionPreview>{
                  submissionId: s.submissionId,
                  answerIndex: i,
                  status: a.status,
                  type: a.answers[0]?.type,
                  previewItem: a.answers[0]?.item,
                  previewText: a.answers[0]?.text,
                  previewStart: a.answers[0]?.start,
                  previewEnd: a.answers[0]?.end
              }))
          } else {
              return [];
          }
        })
    )
  }

    /**
     * Primitive trackBy for SubmissionInfo by the id.
     *
     * Potentially this should include some form of time-information to handle the preview not being ready (yet)
     * @param index
     * @param preview
     */
  public previewById(index: Number, preview: SubmissionPreview){
      let timeout = 30000; //only re-render once every 30 seconds
      if (this.lastTrackMap == null) { //for some reason, this is not necessarily already initialized
          this.lastTrackMap = new Map<string, number>();
      }
      let time = Date.now();
      let id = preview?.submissionId;
      if (!this.lastTrackMap.has(id) || this.lastTrackMap.get(id) < time) {
          this.lastTrackMap.set(id, time + timeout)
      }
      return id + '-' + this.lastTrackMap.get(id);
  }

 /**
  * Returns the number of correct submissions for the provided team.
  *
  * @param teamId The teamId of the team.
  */
  public correctSubmissions(teamId: string): Observable<number> {
      return this.submissionsPerTeam.pipe(
        map((submissions) => submissions.get(teamId)
            .flatMap(s => s.answers).filter((a) => a.status === 'CORRECT').length)
      );
  }

 /**
  * Returns the number of correct submissions for the provided team.
  *
  * @param teamId The teamId of the team.
  */
  public wrongSubmissions(teamId: string): Observable<number> {
     return this.submissionsPerTeam.pipe(
         map((submissions) => submissions.get(teamId)
             .flatMap(s => s.answers).filter((a) => a.status === 'WRONG').length)
     );
  }

 /**
  * Returns the number of correct submissions for the provided team.
  *
  * @param teamId The teamId of the team.
  */
  public indeterminate(teamId: string): Observable<number> {
    return this.submissionsPerTeam.pipe(
        map((submissions) => submissions.get(teamId)
            .flatMap(s => s.answers).filter((a) => a.status === 'INDETERMINATE').length)
    );
  }
}
