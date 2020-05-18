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
    ApexXAxis,
    ApexYAxis,
    ChartComponent
} from 'ng-apexcharts';
import {catchError, filter, map, shareReplay, switchMap} from 'rxjs/operators';



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

    series: ApexAxisChartSeries;
    chart: ApexChart;
    dataLabels: ApexDataLabels;
    plotOptions: ApexPlotOptions;
    xaxis: ApexXAxis;
    yaxis: ApexYAxis;
    stroke: ApexStroke;
    fill: ApexFill;
    legend: ApexLegend;
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
                return this.runService.getApiRunScoreWithRunid(s.id); // TODO Error catching
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

        this.scores.subscribe(value => {
            this.updateChart(value);
        });
    }

    ngOnDestroy(): void {
        this.stateSub.unsubscribe();
        this.scoresSub.unsubscribe();
    }

    private updateChart(scores?: Array<ScoreOverview>) {
        this.xaxis = {
            categories: this.currentTeams.map(t => t.name)
        };
        if (scores) {
            /*
             Transformation for apex.
             In competitionOverview = true mode, ignores are not shown
             In competitionOverview = false mode, ONLY matching taskgroup is shown
             */
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
        } else {
            this.series = [];
        }
    }

    private setupChart() {
        this.chart = {
            type: 'bar',
            stacked: this.competitionOverview // thats why the boolean is setup this way round
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
    }
}
