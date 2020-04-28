import {NgModule} from '@angular/core';
import {MatTableModule} from '@angular/material/table';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatDialogModule} from '@angular/material/dialog';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {MatListModule} from '@angular/material/list';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatMenuModule} from '@angular/material/menu';
import {CompetitionBuilderModule} from '../competition/competition-builder/competition-builder.module';
import {MatSelectModule} from '@angular/material/select';
import {RunViewerComponent} from './run-viewer.component';
import {MatCardModule} from '@angular/material/card';
import {TaskViewerComponent} from './task-viewer.component';
import {TeamsViewerComponent} from './teams-viewer.component';
import {ScoreboardViewerComponent} from './scoreboard-viewer.component';
import {QueryObjectPreviewModule} from './query-object-preview/query-object-preview.module';
import {NgApexchartsModule} from 'ng-apexcharts';

@NgModule({
    imports: [
        MatTableModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        FormsModule,
        ReactiveFormsModule,
        CommonModule,
        MatListModule,
        MatProgressSpinnerModule,
        MatMenuModule,
        CompetitionBuilderModule,
        MatSelectModule,
        MatCardModule,
        QueryObjectPreviewModule,
        NgApexchartsModule
    ],
    exports:      [ RunViewerComponent ],
    declarations: [ RunViewerComponent, TaskViewerComponent, TeamsViewerComponent, ScoreboardViewerComponent ],
    providers:    [ ]
})
export class ViewerModule { }
