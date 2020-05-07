import {AfterViewInit, Component, Input, OnInit, ViewChild} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, ScoreOverview, Team} from '../../../openapi';
import {interval, Observable} from 'rxjs';
import {ApexAxisChartSeries, ApexChart, ApexDataLabels, ApexPlotOptions, ApexXAxis, ApexYAxis, ChartComponent} from 'ng-apexcharts';
import {map, switchMap, withLatestFrom} from 'rxjs/operators';


@Component({
    selector: 'app-scoreboard-viewer',
    templateUrl: './scoreboard-viewer.component.html',
    styleUrls: ['./scoreboard-viewer.component.scss']
})
export class ScoreboardViewerComponent implements OnInit, AfterViewInit {
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;

    @ViewChild('chart') chartComponent: ChartComponent;
    public series: ApexAxisChartSeries;
    public chart: ApexChart;
    public dataLabels: ApexDataLabels;
    public plotOptions: ApexPlotOptions;
    public xaxis: ApexXAxis;
    public yaxis: ApexYAxis;

    teams: Observable<Team[]>;
    currentTeams: Team[];
    scores: Observable<ScoreOverview>;
    private prevScores: ScoreOverview;

    public constructor(
        public runService: CompetitionRunService) {
    }

    ngOnInit(): void {
        this.setupChart();
    }

    ngAfterViewInit(): void {

        /* Get the teams */
        this.teams = this.info.pipe(
            map(i => {
                return i.teams;
            }));
        /* Also store the actually current team */
        this.teams.subscribe(value => {
            this.currentTeams = value;
            this.updateChart();
        });

        /* Get the scores */
        // TODO This is not called?
        this.scores = this.state.pipe(
            switchMap(s => {
                console.log('Original score get');
                return this.runService.getApiRunScoreWithRunidTask(s.id);
            })
        );

        /* every second update score */
        // TODO only when RUNNING, actively polling for scores
        this.scores = interval(1000).pipe(
            withLatestFrom(this.state),
            switchMap(([_, state]) => {
                return this.runService.getApiRunScoreWithRunidTask(state.id);
            }));

        /* Subscribe to changes of the scores, in order to update them */
        this.scores.subscribe(value => {
            /* Check whether score has changed */
            if (this.hasChanged(value)) {
                this.updateChart(value);
                this.prevScores = value;
            }
        });
    }

    private hasChanged(score: ScoreOverview) {
        if (score === undefined) {
            console.log('[TaskScore] Cannot decide whether changed, when undefined scores given');
            return false; // Assuming it should not be undefined
        }
        let out = true; // initially there is no prevScores, so yes, it has changed
        if (this.prevScores !== undefined) {
            out = false;
            score.scores.forEach((s, i) => {
                if (s.score !== this.prevScores.scores[i].score) {
                    out = true;
                }
            });
        }
        return out;
    }

    private updateChart(scores?: ScoreOverview) {
        this.xaxis = {
            categories: this.currentTeams.map(t => t.name),
        };
        /* This seems to be a bug, as the doc says array of colors: https://apexcharts.com/docs/options/yaxis/*/
        /*this.yaxis = {
            labels: {
                style: {
                    colors: this.currentTeams.map(t => t.color)
                }
            }
        };*/
        // https://github.com/apexcharts/apexcharts.js/issues/201
        this.yaxis = {
            labels: {
                style: {
                    colors: '#fff'
                }
            }
        };

        if (scores) {
            this.series = [{
                data: scores.scores.map(score => Number.parseInt(score.score.toFixed(0), 10))
            }];
        }
    }

    private setupChart() {
        /* Basic Settings, horizontal bar chart */
        this.chart = {
            type: 'bar'
        };
        this.plotOptions = {
            bar: {
                horizontal: true,
            }
        };
        /* Apparently, this is required to not throw an error during init of chart*/
        this.xaxis = {
            type: 'category',
            categories: ['']
        };
        this.series = [{data: [0]}];
        // TODO Bar respects color of team
        // TODO Context menu: Apply look and feel of application
        // TODO tooltip / hoverthingy: disable that one, it does not help
    }

}
