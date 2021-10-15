import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {
    CompetitionRunScoresService,
    CompetitionRunService,
    RunInfo,
} from '../../../../openapi';
import {catchError, filter, flatMap, map, shareReplay, switchMap, tap} from 'rxjs/operators';
import {combineLatest, concat, interval, Observable, of} from 'rxjs';
import {
    ApexAxisChartSeries,
    ApexChart,
    ApexLegend, ApexNoData,
    ApexStroke,
    ApexTheme, ApexTitleSubtitle,
    ApexXAxis, ApexYAxis
} from 'ng-apexcharts';

@Component({
    selector: 'app-score-history',
    templateUrl: './run-score-history.component.html',
    styleUrls: ['./run-score-history.component.scss']
})
export class RunScoreHistoryComponent {

    /** Run ID displayed by the current {@link RunScoreHistoryComponent}. */
    private runId: Observable<string>;

    /** List of available scoreboards (by name). */
    public scoreboards: Observable<string[]>;

    /** Title for the time series graph. */
    public title: Observable<ApexTitleSubtitle>;

    /** Run information for the current run ID. */
    public runInfo: Observable<RunInfo>;

    /** Time series data. */
    public series: Observable<ApexAxisChartSeries>;

    /** The currently selected scoreboard. */
    public selectedScoreboard: string = null;

    chart = {
        type: 'line',
        width: '100%',
        height: 700,
        zoom: {
            type: 'x',
            enabled: true,
            autoScaleYaxis: true
        },
        toolbar: {
            autoSelected: 'zoom'
        }
    } as ApexChart;

    stroke: ApexStroke = {
        width: 5,
        curve: 'straight'
    } as ApexStroke;

    xaxis: ApexXAxis = {
        type: 'datetime',
        labels: {
            show : true
        },
        title: {
            text: 'Time'
        }
    } as ApexXAxis;

    yaxis: ApexYAxis = {
        showForNullSeries: false,
        decimalsInFloat: 2,
        title: {
            text: 'Score'
        }
    } as ApexYAxis;

    legend: ApexLegend = {
        position: 'right',
        horizontalAlign: 'left',
        showForSingleSeries: false
    } as ApexLegend;

    theme: ApexTheme = {
        mode: 'dark'
    } as ApexTheme;

    noData: ApexNoData = {
        text: 'No data!'
    };

    /**
     * Default constructor for {@link RunScoreHistoryComponent}
     *
     * @param router The {@link Router} instance used for navigation.
     * @param activeRoute The {@link ActivatedRoute} instance used for navigation.
     * @param runService The {@link CompetitionRunService} used to load run information.
     * @param scoreService The {@link CompetitionRunScoresService} used to load score information
     */
    constructor(private router: Router,
                private activeRoute: ActivatedRoute,
                private runService: CompetitionRunService,
                private scoreService: CompetitionRunScoresService) {

        /* Information about current run. */
        this.runId = this.activeRoute.params.pipe(map(a => a.runId));
        this.runInfo = this.runId.pipe(
            switchMap(runId => this.runService.getApiV1RunInfoWithRunid(runId).pipe(
                catchError((err, o) => {
                    console.log(`[ScoreHistoryComponent] There was an error while loading information in the current run: ${err?.message}`);
                    if (err.status === 404) {
                        this.router.navigate(['/competition/list']);
                    }
                    return of(null);
                }),
                filter(q => q != null)
            )),
            shareReplay({bufferSize: 1, refCount: true})
        );

        this.title = this.runInfo.pipe(
            map(i => {
                return {
                    text: `Score development for ${i.name}.`,
                    align: 'center',
                    style: {
                        fontSize: '28px'
                    }
                } as ApexTitleSubtitle;
            })
        );

        /* List of scoreboard for the current run ID. */
        this.scoreboards = this.runId.pipe(
            switchMap(runId => this.scoreService.getApiV1ScoreRunWithRunidScoreboardList(runId).pipe(
                catchError((err, o) => {
                    console.log(`[ScoreHistoryComponent] There was an error while loading information in the current run: ${err?.message}`);
                    if (err.status === 404) {
                        this.router.navigate(['/competition/list']);
                    }
                    return of([]);
                })
            )),
            tap(s => {
                if (s.length > 0 && this.selectedScoreboard == null) {
                    this.selectedScoreboard = s[0]; /* Default selection. */
                }
            }),
            shareReplay({bufferSize: 1, refCount: true})
        );

        /* Load time series data should be visualized. */
        const scores = this.runId.pipe(
            flatMap(r => interval(2000).pipe(
                switchMap(i => {
                    return this.scoreService.getApiV1ScoreRunWithRunidSeriesWithScoreboard(r, this.selectedScoreboard).pipe(
                        catchError((err, o) => {
                            console.log(`[ScoreHistoryComponent] There was an error while loading scores for run: ${err?.message}`);
                            if (err.status === 404) {
                                this.router.navigate(['/competition/list']);
                            }
                            return of(null);
                        }),
                        filter(q => q != null)
                    );
                })
            )),
            shareReplay({bufferSize: 1, refCount: true})
        );

        /* Prepare time series data. */
        const series = combineLatest([scores, this.runInfo]).pipe(
            map(([data, run]) => {
                /* Prepare data structure for ApexCharts. */
                const array: ApexAxisChartSeries = [];
                for (const s of data) {
                    const team = run.teams.find(t => t.uid === s.team);
                    array.push({name: team.name, data: s.points.map(p => {
                        return {x: p.timestamp, y: p.score, strokeColor: team.color, fillColor: team.color};
                    })});
                }
                return array;
            })
        );
        this.series = concat(of([{name: 'Empty', data: []}]), series);
    }
}
