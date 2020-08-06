import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {CompetitionRunService, RunInfo, RunState, TeamInfo} from '../../../../openapi';
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

    plotOptionsAlt: ApexPlotOptions = {
        bar: {
            horizontal: true,
            distributed: true
        }
    };

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

    legendNone: ApexLegend = {
        show: false
    };

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

    constructor(public runService: CompetitionRunService) {
    }

    ngOnInit(): void {
        /* Create observable from teams. */
        this.teams = this.info.pipe(map(i => i.teams));

        /* Create observable for current task group. */
        this.currentTaskGroup = this.state.pipe(
            map(state => state.currentTask?.taskGroup?.name)
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
                    if (scores == null) {
                        return [{name: 'Empty', data: []}];
                    }
                    return scores.filter(so => {
                        if (this.competitionOverview) {
                            return this.ignoreScores.indexOf(so.name) < 0;
                        } else {
                            return so.taskGroup === taskGroup;
                        }
                    }).map(s => {
                        /* In case there is no value, specifically set 0 as score for each team*/
                        if (s.scores.length === 0) {
                            return {name: s.name, data: team.map(t => {
                                return { x: t.name, y: 0, fillColor: t.color };
                            })};
                        } else {
                            const combined = team.map((t, i) => {
                                return {team: t,  score: Math.round(s.scores[i].score) };
                            }).sort((a, b) => b.score - a.score);
                            return {name: s.name, data: combined.map(c => {
                              return { x: c.team.name, y: c.score, fillColor: c.team.color };
                            })};
                        }
                    });
                })
            ));
    }
}
