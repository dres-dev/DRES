import {AfterViewInit, Component, Input, OnInit, ViewChild} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, ScoreOverview, Team} from '../../../../openapi';
import {interval, Observable} from 'rxjs';
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
import {map, switchMap, withLatestFrom} from 'rxjs/operators';

@Component({
    selector: 'app-competition-scoreboard-viewer',
    templateUrl: './competition-scoreboard-viewer.component.html',
    styleUrls: ['./competition-scoreboard-viewer.component.scss']
})
export class CompetitionScoreboardViewerComponent implements OnInit, AfterViewInit {

    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;
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
    scores: Observable<Array<ScoreOverview>>;

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
        /* Local ref to teams. TODO cleanup: there is a better way */
        this.teams.subscribe(value => {
            this.currentTeams = value;
            this.updateChart();
        });

        /* Regular updates of scores */
        this.scores = interval(1000).pipe(
            withLatestFrom(this.state),
            switchMap(([_, state]) => {
                return this.runService.getApiRunScoreWithRunid(state.id);
            }));

        this.scores.subscribe(value => {
            if (this.hasChanged(value)) {
                this.updateChart(value);
                this.prevScores = value;
            }
        });
    }

    private hasChanged(scores: Array<ScoreOverview>) {
        if (scores === undefined) {
            console.log('[Com.Score] Cannot decide whether changed, when undefined scores given');
            return false; // Assuming it should not be undefined
        }
        let out = true;
        if (this.prevScores !== undefined) {
            out = JSON.stringify(this.prevScores) !== JSON.stringify(scores);
        }
        return out;
    }

    private updateChart(scores?: Array<ScoreOverview>) {
        this.xaxis = {
            categories: this.currentTeams.map(t => t.name)
        };

        if (scores) {
            /* transform scores for apex, do not include the ignores */
            this.series = scores.filter(so => {
                return this.ignoreScores.indexOf(so.name) < 0;
            }).map(s => {
                return {name: s.name, data: s.scores.map(sc => sc.score)};
            });
        }
    }

    private setupChart() {
        this.chart = {
            type: 'bar',
            stacked: true
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
        /* To have readable lables, see scorebaordview */
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
