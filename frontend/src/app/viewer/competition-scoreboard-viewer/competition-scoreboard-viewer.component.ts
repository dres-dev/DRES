import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {CompetitionRunScoresService, RunInfo, RunState, Score, ScoreOverview, TeamInfo} from '../../../../openapi';
import {combineLatest, concat, Observable, of} from 'rxjs';
import {
    ApexAxisChartSeries,
    ApexChart,
    ApexDataLabels,
    ApexFill,
    ApexLegend,
    ApexPlotOptions,
    ApexStroke,
    ApexTheme,
    ChartComponent
} from 'ng-apexcharts';
import {catchError, map, switchMap} from 'rxjs/operators';


/**
 * Component displaying a lovely scoreboard.
 * There are two modes:
 * competitionOverview = true -- In this mode, a stacked bar chart over all task groups is shown
 * competitionOverview = false -- In this mode, a bar chart of the current task group is shown
 */
@Component({
    selector: 'app-competition-scoreboard-viewer',
    templateUrl: './competition-scoreboard-viewer.component.html',
    styleUrls: ['./competition-scoreboard-viewer.component.scss']
})
export class CompetitionScoreboardViewerComponent implements OnInit {

    /**
     * The run info of the current run
     */
    @Input() info: Observable<RunInfo>;

    /**
     * The observable for the state, which is updated through a websocket
     */
    @Input() state: Observable<RunState>;

    /**
     * Whether or not to show competition overview scores.
     * Otherwise, the current task group total is shown.
     */
    @Input() competitionOverview = true;

    @ViewChild('chart') chartComponent: ChartComponent;

    chart: ApexChart = {
        type: 'bar',
        stacked: this.competitionOverview, // that's why the boolean is setup this way
        animations: {
            enabled: false,
            dynamicAnimation: {
                enabled: false
            }
        }
    } as ApexChart;

    plotOptions: ApexPlotOptions = {
        bar: {
            horizontal: true
        }
    } as ApexPlotOptions;

    stroke: ApexStroke = {
        width: 1,
        colors: ['#fff']
    } as ApexStroke;

    fill: ApexFill = {
        opacity: 1
    } as ApexFill;

    legend: ApexLegend = {
        position: 'top',
        horizontalAlign: 'left',
        showForSingleSeries: false
    } as ApexLegend;

    theme: ApexTheme = {
        mode: 'dark',
        palette: 'palette10'
    } as ApexTheme;

    dataLabels: ApexDataLabels = {
        enabled: true
    } as ApexDataLabels;

    series: Observable<ApexAxisChartSeries>;

    teams: Observable<TeamInfo[]>;
    currentTaskGroup: Observable<string>;

    // TODO Make this somewhat more beautiful and configurable
    private ignoreScores = ['average'];

    constructor(public scoreService: CompetitionRunScoresService) {}

    ngOnInit(): void {
        /* Create observable from teams. */
        this.teams = this.info.pipe(map(i => i.teams));

        /* Create observable for current task group. */
        this.currentTaskGroup = this.state.pipe(
            map(state => state.currentTask?.taskGroup)
        );

        if (this.competitionOverview) {
            /* Create observable for series. */
            this.series = this.competitionOverviewSeries();
        } else {
            this.series = this.taskGroupOverview();
        }
    }

    /**
     * Generates and returns an observable {@link ApexAxisChartSeries} for the
     * score overview grouped by task group
     *
     * @return {@link Observable<ApexAxisChartSeries>} The observable.
     */
    private taskGroupOverview(): Observable<ApexAxisChartSeries> {
        /* Download scores. */
        const score = this.state.pipe(
            switchMap(s => {
                return this.scoreService.getApiV1ScoreRunWithRunidCurrent(s.id).pipe(
                    catchError(err => {
                        console.log('Error when retrieving scores.', err);
                        return of(null);
                    })
                );
            })
        );

        /* Generate series. */
        const series = combineLatest([score, this.teams, this.currentTaskGroup]).pipe(
            map(([scores, team, taskGroup]) => {
                if (scores == null) {
                    /* If we know the team, at least we can zero the teams */
                    if (team != null) {
                        return [{
                            name: 'N/A', data: team.map(t => {
                                return {x: t.name, y: 0, fillColor: t.color};
                            })
                        }] as ApexAxisChartSeries;
                    } else {
                        return [{name: 'Empty', data: []}];
                    }
                }
                /* In case there is no value, specifically set 0 as score for each team*/
                if (scores.scores.length === 0) {
                    return [{
                        name: scores.name, data: team.map(t => {
                            return {x: t.name, y: 0, fillColor: t.color};
                        })
                    }] as ApexAxisChartSeries;
                } else {
                    const combined = team.map((t, i) => {
                        return {team: t, score: Math.round(scores.scores[i].score)};
                    }).sort((a, b) => b.score - a.score);
                    return [{
                        name: scores.name, data: combined.map(c => {
                            return {x: c.team.name, y: c.score, fillColor: c.team.color};
                        })
                    }] as ApexAxisChartSeries;
                }
            })
        );

        /* Return an empty series first, otherwise ApexChars will fail. */
        return concat(of([{name: 'Empty', data: []}]), series);
    }

    /**
     * Generates and returns an observable {@link ApexAxisChartSeries} score overview for the current task.
     *
     * @return {@link Observable<ApexAxisChartSeries>} The observable.
     */
    private competitionOverviewSeries(): Observable<ApexAxisChartSeries> {
        /* Fetch scores. */
        const score: Observable<Array<ScoreOverview>> = this.state.pipe(
            switchMap(s => {
                return this.scoreService.getApiV1ScoreRunWithRunid(s.id).pipe(
                    catchError(err => {
                        console.log('Error when retrieving scores.', err);
                        return of(null);
                    }),
                    map(scores => scores.filter(so => this.ignoreScores.indexOf(so.name) < 0))
                );
            })
        );

        /* Generate series. */
        const series = combineLatest([score, this.teams]).pipe(
            map(([scores, team]) => {
                if (scores == null) {
                    return [{name: 'Empty', data: []}];
                }

                /* Array of teams ordered by maximum score. */
                const teamsOrdered = team.map(t => {
                    const sum = scores.map(so => so.scores.find(s => s.teamId === t.uid)).reduce((a, b) => {
                        return {teamId: a.teamId, score: a.score + b.score} as Score;
                    });
                    return {team: t, score: sum};
                }).sort((t1, t2) => t2.score.score - t1.score.score);

                /* Array of per category scores for each team. */
                return scores.filter(s => {
                    return s.name !== 'sum';
                }).map(s => {
                    /* In case there is no value, specifically set 0 as score for each team*/
                    if (s.scores.length === 0) {
                        return {
                            name: s.name, data: teamsOrdered.map(t => {
                                return {x: t.team.name, y: 0};
                            })
                        };
                    } else {
                        return {
                            name: s.name, data: teamsOrdered.map(t => {
                                return {x: t.team.name, y: Math.round(s.scores.find(ss => ss.teamId === t.team.uid).score)};
                            })
                        };
                    }
                });
            })
        );

        /* Return an empty series first, otherwise ApexChars will fail. */
        return concat(of([{name: 'Empty', data: []}]), series);
    }
}
