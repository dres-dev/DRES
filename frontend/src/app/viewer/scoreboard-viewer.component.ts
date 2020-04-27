import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {DefaultService, RunInfo, RunState} from '../../../openapi';
import {Observable} from 'rxjs';
import {ApexAxisChartSeries, ApexChart, ApexDataLabels, ApexPlotOptions, ApexXAxis, ChartComponent} from 'ng-apexcharts';


@Component({
    selector: 'app-scoreboard-viewer',
    templateUrl: './scoreboard-viewer.component.html',
    styleUrls: ['./scoreboard-viewer.component.scss']
})
export class ScoreboardViewerComponent implements OnInit {
    @Input() info: Observable<RunInfo>;
    @Input() state: Observable<RunState>;

    @Input() enabled: boolean;

    @ViewChild('chart') chartComponent: ChartComponent;
    public series: ApexAxisChartSeries;
    public chart: ApexChart;
    public dataLabels: ApexDataLabels;
    public plotOptions: ApexPlotOptions;
    public xaxis: ApexXAxis;


    public constructor(public defaultService: DefaultService) {

    }

    ngOnInit(): void {
        if (!this.enabled) {
            return;
        }
        /* Basic Settings, horizontal bar chart */
        this.chart = {
            type: 'bar'
        };
        this.plotOptions = {
            bar: {
                horizontal: true,
            }
        };

        this.xaxis = {
            categories: [
                'team1', 'team2', 'team3' // team
            ]
        };
        this.series = [{
            data: [100, 200, 300] // Score
        }];


    }

}
