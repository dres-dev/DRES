import {AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, ScoreOverview, Team} from '../../../../openapi';
import {Observable, of, Subscription} from 'rxjs';
import {
    ApexAxisChartSeries,
    ApexChart,
    ApexDataLabels,
    ApexFill,
    ApexLegend,
    ApexPlotOptions,
    ApexStroke,
    ApexTheme,
    ApexXAxis,
    ApexYAxis,
    ChartComponent
} from 'ng-apexcharts';
import {catchError, filter, map, shareReplay, switchMap, tap} from 'rxjs/operators';


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
export class CompetitionScoreboardViewerComponent implements OnInit, AfterViewInit, OnDestroy {

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

    series: Partial<ApexAxisChartSeries>;
    chart: Partial<ApexChart>;
    dataLabels: Partial<ApexDataLabels>;
    plotOptions: Partial<ApexPlotOptions>;
    xaxis: Partial<ApexXAxis>;
    yaxis: Partial<ApexYAxis>;
    stroke: Partial<ApexStroke>;
    fill: Partial<ApexFill>;
    legend: Partial<ApexLegend>;
    theme: Partial<ApexTheme>;

    teams: Observable<Team[]>;
    currentTeams: Team[];
    currentTaskGroup: string;
    scores: Observable<Array<ScoreOverview>>;

    private stateSub: Subscription;
    private scoresSub: Subscription;

    // TODO Make this somewhat more beautiful and configurable
    private ignoreScores = ['average'];
    private prevScores: Array<ScoreOverview>;

    constructor(
        public runService: CompetitionRunService
    ) {
    }

    ngOnInit(): void {
        this.setupChart();
    }

    ngAfterViewInit(): void {
        /* Get the teams */
        this.teams = this.info.pipe(
            map(i => {
                return i.teams;
            })
        );
        /* Local ref to teams.*/
        this.teams.subscribe(value => {
            this.currentTeams = value;
            this.updateChart();
        });


        /* Get the socres */
        this.scores = this.state.pipe(
            switchMap(s => {
                return this.runService.getApiRunScoreWithRunid(s.id).pipe(
                    tap(res => {
                        console.log(`ScoreWithRunId: ${JSON.stringify(res)}`);
                        return res;
                    }),
                    catchError(err => {
                        console.log('Error in Scores: ');
                        console.log(err);
                        return of(null);
                    })
                );
            }),
            catchError(err => {
                console.log(`Error: ${err}`);
                return of(null);
            }),
            /* Fires only if actually scores are present */
            filter(value => value != null),
            shareReplay(1)
        );
        this.stateSub = this.state.subscribe(state => {
            this.currentTaskGroup = state.currentTask?.taskGroup.name;
        });

        this.scoresSub = this.scores.subscribe(value => {
            this.updateChart(value);
        });
    }

    ngOnDestroy(): void {
        this.stateSub.unsubscribe();
        this.scoresSub.unsubscribe();
    }

    private updateChart(scores?: Array<ScoreOverview>) {
        console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] Updating scores`);
        this.xaxis = {
            categories: this.currentTeams.map(t => t.name)
        };
        if (scores) {
            console.log(scores);
            /*
             Transformation for apex.
             In competitionOverview = true mode, ignores are not shown
             In competitionOverview = false mode, ONLY matching taskgroup is shown
             */
            if (scores.length > 1) {
                console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] Multiple Scores`);
                this.series = scores.filter(so => {
                    if (this.competitionOverview) {
                        return this.ignoreScores.indexOf(so.name) < 0;
                    } else {
                        return so.taskGroup === this.currentTaskGroup;
                    }
                }).map(s => {
                    /* In case there is no value, specifically set 0 as score for each team*/
                    if (s.scores.length === 0) {
                        return {name: s.name, data: this.currentTeams.map(t => 0)};
                    } else {
                        return {name: s.name, data: s.scores.map(sc => Math.round(sc.score))};
                    }
                });
            } else if (scores[0] !== undefined) {
                console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] First`);
                this.series = [{name: scores[0].name, data: scores[0].scores.map(sc => Math.round(sc.score))}];
            } else {
                // TODO check with @ppanopticon why
                if (scores.hasOwnProperty('name') && scores.hasOwnProperty('scores')) {
                    const so = (scores as unknown) as ScoreOverview;
                    if (this.competitionOverview) {
                        console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] Overview scores`);
                        if (this.ignoreScores.indexOf(so.name) < 0){

                        }
                    } else {
                        console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] Taskgroup Scores`);
                        if (so?.taskGroup === this.currentTaskGroup) { // ?. due to 'average' has taskGroup === null
                            this.series = [{name: this.currentTaskGroup, data: so.scores.map(sc => Math.round(sc.score))}];
                        }
                    }
                } else {
                    console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] No scores`);
                    this.setChartsToZero();

                }
            }

        } else {
            this.setChartsToZero();
        }
    }

    private setChartsToZero() {
        // TODO sensible zeros for competitionOverview
        /*if (this.competitionOverview) {
            this.series = [];
        } else {*/
        this.series = [{name: 'Empty', data: this.currentTeams.map(_ => 0)}];
        // }
    }

    private setupChart() {
        this.chart = {
            type: 'bar',
            stacked: this.competitionOverview, // thats why the boolean is setup this way round
            animations: {
                enabled: false,
                dynamicAnimation: {
                    enabled: false
                }
            }
        };
        this.plotOptions = {
            bar: {
                horizontal: true
            }
        };
        this.stroke = {
            width: 1,
            colors: ['#fff']
        };
        /* Apparently required to not throw errors */
        this.xaxis = {
            categories: ['']
        };
        /* Apparently, labels still have to be colored this way, even though 'horizontal' bar is just flipped */
        this.yaxis = {
            labels: {
                style: {
                    colors: '#fff'
                }
            }
        };
        this.fill = {
            opacity: 1
        };
        this.legend = {
            position: 'top',
            horizontalAlign: 'left',
            labels: {
                colors: '#fff'
            }
        };
        // Apparently, this is not fully supported https://github.com/apexcharts/apexcharts.js/issues/218
        // hack: See style.css
        this.theme = {
            mode: 'dark'
        };
        this.series = [{data: [0]}];
    }
}
