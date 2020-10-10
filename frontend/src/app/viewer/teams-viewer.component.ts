import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, ViewChild} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, ScoreOverview, SubmissionInfo, TaskInfo} from '../../../openapi';
import {BehaviorSubject, merge, Observable, of, Subscription} from 'rxjs';
import {catchError, filter, flatMap, map, pairwise, retry, shareReplay, switchMap, tap, withLatestFrom, combineLatest} from 'rxjs/operators';
import {AppConfig} from '../app.config';
import {AudioPlayerUtilities} from '../utilities/audio-player.utilities';
import {animate, keyframes, style, transition, trigger} from '@angular/animations';

/**
 * Internal helper interface.
 */
interface SubmissionDelta {
    correct: number[];
    wrong: number [];
}

@Component({
    selector: 'app-teams-viewer',
    templateUrl: './teams-viewer.component.html',
    styleUrls: ['./teams-viewer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
    trigger('highlight', [
        transition('nohighlight => correct', animate('1500ms', keyframes([
            style({backgroundColor: 'initial', offset: 0} ),
            style({backgroundColor: 'lightgreen', offset: 0.1} ),
            style({backgroundColor: 'initial', offset: 1} ),
        ]))),
        transition('nohighlight => wrong', animate('1500ms', keyframes([
            style({backgroundColor: 'initial', offset: 0} ),
            style({backgroundColor: 'tomato', offset: 0.1} ),
            style({backgroundColor: 'initial', offset: 1} ),
        ])))
    ])
    ]
})
export class TeamsViewerComponent implements AfterViewInit, OnDestroy {
    @Input() runId: Observable<string>;
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;
    @Input() taskEnded: Observable<TaskInfo>;

    /** Observable that tracks all the submissions per team. */
    submissions: Observable<SubmissionInfo[][]>;

    /** Observable that tracks the current score per team. */
    scores: Observable<ScoreOverview>;

    /** Observable that tracks whether a highlight animation should be played for the given team. */
    highlight: Observable<string[]>;

    /** Behaviour subject used to reset highlight animation state. */
    resetHighlight: BehaviorSubject<void> = new BehaviorSubject(null);

    /** Reference to the audio file played during countdown. */
    @ViewChild('audio') audio: ElementRef<HTMLAudioElement>;

    /** Internal subscription for playing sound effect of a task that has ended. */
    taskEndedSoundEffect: Subscription;

    constructor(private runService: CompetitionRunService,
                private ref: ChangeDetectorRef,
                public config: AppConfig) {

        this.ref.detach();
        setInterval(() => {
            this.ref.detectChanges();
        }, 500);
    }

    ngAfterViewInit(): void {
        /* Observable that tracks all the submissions per team. */
        this.submissions = this.state.pipe(
            switchMap(st => this.runService.getApiRunWithRunidTaskSubmissionList(st.id).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[TeamsViewerComponent] Error while loading submissions: ${err?.message}.`);
                    return of(null);
                }),
                filter(sb => sb != null), /* Filter null responses. */
            )),
            combineLatest(this.info),
            map(([submissions, info]) => {
                return info.teams.map((v, i) => {
                    return submissions.filter(s => s.team === i);
                });
            }),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading of submission. */
        );

        /* Observable that tracks the current score per team. */
        this.scores = this.state.pipe(
            switchMap(st => this.runService.getApiRunScoreWithRunidTask(st.id).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[TeamsViewerComponent] Error while loading scores: ${err?.message}.`);
                    return of(null);
                }),
                filter(sc => sc != null), /* Filter null responses. */
            )),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading of score. */
        );

        /* Observable that calculates changes to the submission per team every 250ms. */
        const submissionDelta = this.submissions.pipe(
            pairwise(),
            map(([s1, s2]) => {
                const delta = {
                    s1_correct: s1.map(s => s.filter(ss => ss.status === 'CORRECT').length),
                    s1_wrong: s1.map(s => s.filter(ss => ss.status === 'WRONG').length),

                    s2_correct: s2.map(s => s.filter(ss => ss.status === 'CORRECT').length),
                    s2_wrong: s2.map(s => s.filter(ss => ss.status === 'WRONG').length)
                };
                return {
                    correct: delta.s1_correct.map((s, i) => delta.s2_correct[i] - s),
                    wrong: delta.s1_wrong.map((s, i) => delta.s2_wrong[i] - s)
                } as SubmissionDelta;
            }));


        /* Observable that indicates whether a certain team has new submissions. */
        this.highlight = merge(submissionDelta.pipe(
            map(delta => {
                const correct = delta.correct.map(s => s > 0);
                const wrong = delta.wrong.map(s => s > 0);
                return correct.map((s, i) => {
                    if (s === true) {
                        return 'correct';
                    } else if (wrong[i]) {
                        return 'wrong';
                    } else {
                        return 'nohighlight';
                    }
                }); /* Zip two arrays and calculate logical OR. */
            }),
            tap(s => {
                if (s.filter(e => e === 'correct').length > 0) {
                    AudioPlayerUtilities.playOnce('assets/audio/correct.ogg', this.audio.nativeElement);
                } else if (s.filter(e => e === 'wrong').length > 0) {
                    AudioPlayerUtilities.playOnce('assets/audio/wrong.ogg', this.audio.nativeElement);
                }
            })),
            this.resetHighlight.pipe(
                flatMap(() => this.info.pipe(map(i => i.teams.map(t => 'nohighlight'))))
            )
        )
        .pipe(shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading of score. */);

        /** Subscription for end of task (used to play sound effects). */
        this.taskEndedSoundEffect = this.taskEnded.pipe(
            withLatestFrom(this.submissions),
            map(([task, submission]) => {
                return submission.filter(s => (s.filter(ss => ss.status === 'CORRECT').length) > 0).length > 0;
            })
        ).subscribe(success => {
            if (success) {
                AudioPlayerUtilities.playOnce('assets/audio/applause.ogg', this.audio.nativeElement);
            } else {
                AudioPlayerUtilities.playOnce('assets/audio/sad_trombone.ogg', this.audio.nativeElement);
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
    public previewForSubmission(submission: SubmissionInfo): Observable<string> {
        return this.runId.pipe(map(runId => this.config.resolveApiUrl(`/preview/submission/${runId}/${submission.id}`)));
    }

    /**
     * Generates a URL for the logo of the team.
     */
    public teamLogo(teamId: number): Observable<string> {
        return this.runId.pipe(map(runId => this.config.resolveApiUrl(`/run/logo/${runId}/${teamId}`)));
    }

    /**
     * Returns an obsevable for the {@link SubmissionInfo} for the given team.
     *
     * @param team The team's index.
     */
    public submissionForTeam(team: number): Observable<SubmissionInfo[]> {
        return this.submissions.pipe(
            map(s => {
                if (s != null) {
                    return s[team];
                } else {
                    return [];
                }
            })
        );
    }

    public score(team: number): Observable<string> {
        return this.scores.pipe(
            filter(s => s != null),
            map(scores => scores.scores.find(s => s.teamId === team)?.score.toFixed(0))
        );
    }

    public correctSubmissions(team: number): Observable<number> {
        return this.submissions.pipe(
            map(submissions => submissions[team].filter(s => s.status === 'CORRECT').length)
        );
    }

    public wrongSubmissions(team: number): Observable<number> {
        return this.submissions.pipe(
            map(submissions => submissions[team].filter(s => s.status === 'WRONG').length)
        );
    }

    public indeterminate(team: number): Observable<number> {
        return this.submissions.pipe(
            map(submissions => submissions[team].filter(s => s.status === 'INDETERMINATE').length)
        );
    }
}
