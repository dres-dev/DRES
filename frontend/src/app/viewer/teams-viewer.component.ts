import {AfterViewInit, Component, Input, OnDestroy} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, ScoreOverview, SubmissionInfo, TaskDescription} from '../../../openapi';
import {Observable, of, Subscription} from 'rxjs';
import {catchError, filter, map, retry, shareReplay, switchMap, withLatestFrom} from 'rxjs/operators';
import {AppConfig} from '../app.config';

@Component({
    selector: 'app-teams-viewer',
    templateUrl: './teams-viewer.component.html',
    styleUrls: ['./teams-viewer.component.scss']
})
export class TeamsViewerComponent implements AfterViewInit, OnDestroy {
    @Input() runId: Observable<number>;
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;
    @Input() taskEnded: Observable<TaskDescription>;

    submissions: Observable<SubmissionInfo[][]>;
    scores: Observable<ScoreOverview>;
    successSubscription: Subscription;


    /** Reference to the audio file played during countdown. */
    audio = [
        new Audio(), /** Success. */
        new Audio() /** Failure. */
    ];
    constructor(private runService: CompetitionRunService, private config: AppConfig) {
        this.audio[0].src = './assets/audio/applause.ogg';
        this.audio[0].load();
        this.audio[1].src = './assets/audio/sad_trombone.ogg';
        this.audio[1].load();
    }



    ngAfterViewInit(): void {
        this.submissions = this.state.pipe(
            switchMap(st => this.runService.getApiRunWithRunidTaskSubmissions(st.id).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[TeamsViewerComponent] Error while loading submissions: ${err?.message}.`);
                    return of(null);
                }),
                filter(sb => sb != null), /* Filter null responses. */
            )),
            withLatestFrom(this.info),
            map(([submissions, info]) => {
                return info.teams.map((v, i) => {
                    return submissions.filter(s => s.team === i);
                });
            }),
            shareReplay({bufferSize: 1, refCount: true}) /* Cache last successful loading of submission. */
        );

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

        /** Subscription for end of task (used to play sound effects). */
        this.successSubscription = this.taskEnded.pipe(
            withLatestFrom(this.submissions),
            map(([task, submission]) => {
                return submission.filter(s => (s.filter(ss => ss.status === 'CORRECT').length) > 0).length > 0;
            })
        ).subscribe(success => {
            if (success) {
                this.audio[0].play().then(r => {});
            } else {
                this.audio[1].play().then(r => {});
            }
        });
    }

    public ngOnDestroy(): void {
        this.successSubscription.unsubscribe();
        this.successSubscription = null;
    }


    /**
     * Generates a URL for the preview image of a submission.
     *
     * @param submission
     */
    public previewForSubmission(submission: SubmissionInfo): Observable<string> {
        return this.runId.pipe(map(runId => this.config.resolveApiUrl(`/preview/submission/${runId}/${submission.id}`)));
    }

    /**
     *
     * @param team
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

