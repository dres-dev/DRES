import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, ScoreOverview, Team} from '../../../../openapi';
import {concat, Observable, of} from 'rxjs';
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
import {catchError, map, switchMap, withLatestFrom} from 'rxjs/operators';


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
        stacked: this.competitionOverview, // thats why the boolean is setup this way round
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

    yaxis: ApexYAxis = {
        labels: {
            style: {
                colors: '#fff'
            }
        }
    } as ApexYAxis;

    stroke: ApexStroke = {
        width: 1,
        colors: ['#fff']
    } as ApexStroke;

    fill: ApexFill = {
        opacity: 1
    } as ApexFill;

    legend: ApexLegend =  {
        position: 'top',
        horizontalAlign: 'left',
        labels: {
            colors: '#fff'
        }
    } as ApexLegend;

    theme: ApexTheme = {
        mode: 'dark'
    } as ApexTheme;

    dataLabels: ApexDataLabels = {
        enabled: false
    } as ApexDataLabels;

    xaxis: Observable<Partial<ApexXAxis>>;
    series: Observable<Partial<ApexAxisChartSeries>>;

    teams: Observable<Team[]>;
    currentTaskGroup: Observable<string>;

    // TODO Make this somewhat more beautiful and configurable
    private ignoreScores = ['average'];

    constructor(public runService: CompetitionRunService) {}

    ngOnInit(): void {
        /* Create observable from teams. */
        this.teams = this.info.pipe(map(i => i.teams));

        /* Create observable for current task group. */
        this.currentTaskGroup = this.state.pipe(
            map(state => state.currentTask?.taskGroup.name)
        );

        /* Create observable for x-Axis data. */
        this.xaxis = this.teams.pipe(
            map(team => {
                return { categories: team.map(t => t.name) };
            })
        );

        /* Create observable for series. */
        this.series = concat(
            of([{name: 'Empty', data: []}]),
            this.state.pipe(
            switchMap(s => {
                return this.runService.getApiRunScoreWithRunid(s.id).pipe(
                    catchError(err => {
                        console.log('Error when retrieving scores.', err);
                        return of(null);
                    })
                );
            }),
            withLatestFrom(this.teams, this.currentTaskGroup),
            map(([scores, team, taskGroup]) => {
                if (scores && scores.length > 0) {
                    console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] Multiple Scores`);
                    return scores.filter(so => {
                        if (this.competitionOverview) {
                            return this.ignoreScores.indexOf(so.name) < 0;
                        } else {
                            return so.taskGroup === taskGroup;
                        }
                    }).map(s => {
                        /* In case there is no value, specifically set 0 as score for each team*/
                        if (s.scores.length === 0) {
                            return {name: s.name, data: team.map(t => 0)};
                        } else {
                            return {name: s.name, data: s.scores.map(sc => Math.round(sc.score))};
                        }
                    });
                } else {
                    // TODO check with @ppanopticon why
                    if (scores.hasOwnProperty('name') && scores.hasOwnProperty('scores')) {
                        const so = (scores as unknown) as ScoreOverview;
                        if (this.competitionOverview) {
                            console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] Overview scores`);
                            if (this.ignoreScores.indexOf(so.name) < 0) { }
                        } else {
                            console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] Taskgroup Scores`);
                            if (so?.taskGroup === taskGroup) { // ?. due to 'average' has taskGroup === null
                                return [{name: taskGroup, data: so.scores.map(sc => Math.round(sc.score))}];
                            }
                        }
                    } else {
                        console.log(`[${this.competitionOverview ? 'Competition' : 'Taskgroup'}Scoreboard] No scores`);
                        return [{name: 'Empty', data: team.map(_ => 0)}];
                    }
                }
            })
        ));
    }
}
