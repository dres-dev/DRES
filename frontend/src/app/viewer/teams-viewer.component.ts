import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, ViewChild} from '@angular/core';
import {
    CompetitionRunScoresService,
    CompetitionRunService,
    RunInfo,
    RunState,
    ScoreOverview,
    SubmissionInfo,
    TaskInfo,
    TeamInfo
} from '../../../openapi';
import {BehaviorSubject, combineLatest, merge, Observable, of, Subscription} from 'rxjs';
import {catchError, filter, flatMap, map, pairwise, retry, shareReplay, switchMap, withLatestFrom} from 'rxjs/operators';
import {AppConfig} from '../app.config';
import {AudioPlayerUtilities} from '../utilities/audio-player.utilities';
import {animate, keyframes, style, transition, trigger} from '@angular/animations';

/**
 * Internal helper interface.
 */
interface SubmissionDelta {
    correct: number;
    wrong: number ;
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

    /** Observable that tracks all the submissions. */
    submissions: Observable<SubmissionInfo[]>;

    /** Observable that tracks all the submissions per team. */
    submissionsPerTeam: Observable<Map<string, SubmissionInfo[]>>;

    /** Observable that tracks the current score per team. */
    scores: Observable<Map<string, number>>;

    /** Observable that tracks whether a highlight animation should be played for the given team. */
    highlight: Observable<Map<string, string>>;

    /** Behaviour subject used to reset highlight animation state. */
    resetHighlight: BehaviorSubject<void> = new BehaviorSubject(null);

    /** Reference to the audio file played during countdown. */
    @ViewChild('audio') audio: ElementRef<HTMLAudioElement>;

    /** Internal subscription for playing sound effect of a task that has ended. */
    taskEndedSoundEffect: Subscription;

    constructor(private runService: CompetitionRunService,
                private scoresService: CompetitionRunScoresService,
                private ref: ChangeDetectorRef,
                public config: AppConfig) {

        this.ref.detach();
        setInterval(() => {
            this.ref.detectChanges();
        }, 500);
    }

    ngAfterViewInit(): void {
        /* Create source observable; list of all submissions.  */
        this.submissions = this.state.pipe(
            switchMap(st => this.runService.getApiV1RunWithRunidSubmissions(st.id).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[TeamsViewerComponent] Error while loading submissions: ${err?.message}.`);
                    return of(null);
                }),
                filter(sb => sb != null), /* Filter null responses. */
            )),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading of submission. */
        );

        /* Observable that tracks all the submissions per team. */
        this.submissionsPerTeam = combineLatest([this.submissions, this.info]).pipe(
            map(([submissions, info]) => {
                const submissionsPerTeam = new Map<string, SubmissionInfo[]>();
                info.teams.forEach(t => {
                    submissionsPerTeam.set(t.uid, submissions.filter(s => s.teamId === t.uid));
                });
                return submissionsPerTeam;
            }),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading of submission. */
        );

        /* Observable that tracks the current score per team. */
        this.scores = this.state.pipe(
            switchMap(st => this.scoresService.getApiV1ScoreRunWithRunidCurrent(st.id).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[TeamsViewerComponent] Error while loading scores: ${err?.message}.`);
                    return of(null);
                }),
                filter(sc => sc != null), /* Filter null responses. */
            )),
            map((sc: ScoreOverview) => {
                const scores = new Map<string, number>();
                sc.scores.forEach((v) => scores.set(v.teamId, v.score));
                return scores;
            }),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading of score. */
        );

        /* Observable that calculates changes to the submission every 250ms (for sound effects playback). */
        const submissionDelta: Observable<Map<string, SubmissionDelta>> = this.submissionsPerTeam.pipe(
            pairwise(),
            map(([s1, s2]) => {
                const delta = new Map<string, SubmissionDelta>();
                for (const [key, value] of s1) {
                    delta.set(key, {
                        correct: Math.max(s2.get(key).filter(s => s.status === 'CORRECT').length -  value.filter(s => s.status === 'CORRECT').length, 0),
                        wrong: Math.max(s2.get(key).filter(s => s.status === 'WRONG').length - value.filter(s => s.status === 'WRONG').length, 0),
                    } as SubmissionDelta);
                }
                return delta;
            }));

        /* Observable that indicates whether a certain team has new submissions. */
        this.highlight = merge(submissionDelta.pipe(
            map(delta => {
                const highlight = new Map<string, string>();
                for (const [key, value] of delta) {
                    if (value.correct > value.wrong) {
                        highlight.set(key, 'correct');
                        AudioPlayerUtilities.playOnce('/immutable/assets/audio/correct.ogg', this.audio.nativeElement);
                    } else if (value.wrong > value.correct) {
                        highlight.set(key, 'wrong');
                        AudioPlayerUtilities.playOnce('/immutable/assets/audio/wrong.ogg', this.audio.nativeElement);
                    } else {
                        highlight.set(key, 'nohighlight');
                    }
                }
                return highlight;
            })),
            this.resetHighlight.pipe(
                flatMap(() => this.info),
                map(info => {
                    const hightlight = new Map<string, string>();
                    info.teams.forEach(t => hightlight.set(t.uid, 'nohighlight'));
                    return hightlight;
                })
            )
        ).pipe(shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading of score. */);

        /* Subscription for end of task (used to play sound effects). */
        this.taskEndedSoundEffect = this.taskEnded.pipe(
            withLatestFrom(this.submissions),
            map(([ended, submissions]) => {
                for (const s of submissions) {
                    if (s.status === 'CORRECT') { return true; }
                }
                return false;
            })
        ).subscribe(success => {
            if (this.audio) {
                if (success) {
                    AudioPlayerUtilities.playOnce('immutable/assets/audio/applause.ogg', this.audio.nativeElement);
                } else {
                    AudioPlayerUtilities.playOnce('immutable/assets/audio/sad_trombone.ogg', this.audio.nativeElement);
                }
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
    public teamLogo(team: TeamInfo): string {
        return this.config.resolveApiUrl(`/competition/logo/${team.logoId}`);
    }

    /**
     * Returns an observable for the {@link SubmissionInfo} for the given team.
     *
     * @param teamId The team's uid.
     */
    public submissionForTeam(teamId: string): Observable<SubmissionInfo[]> {
        return this.submissionsPerTeam.pipe(
            map(s => {
                if (s != null) {
                    return s.get(teamId);
                } else {
                    return [];
                }
            })
        );
    }

    public correctSubmissions(teamId: string): Observable<number> {
        return this.submissionsPerTeam.pipe(
            map(submissions => submissions.get(teamId).filter(s => s.status === 'CORRECT').length)
        );
    }

    public wrongSubmissions(teamId: string): Observable<number> {
        return this.submissionsPerTeam.pipe(
            map(submissions => submissions.get(teamId).filter(s => s.status === 'WRONG').length)
        );
    }

    public indeterminate(teamId: string): Observable<number> {
        return this.submissionsPerTeam.pipe(
            map(submissions => submissions.get(teamId).filter(s => s.status === 'INDETERMINATE').length)
        );
    }
}
