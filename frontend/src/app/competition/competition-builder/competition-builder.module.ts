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
import {CompetitionBuilderComponent} from './competition-builder.component';
import {CompetitionBuilderTeamDialogComponent} from './competition-builder-team-dialog/competition-builder-team-dialog.component';
import {CompetitionBuilderTaskDialogComponent} from './competition-builder-task-dialog/competition-builder-task-dialog.component';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatSelectModule} from '@angular/material/select';
import {CompetitionBuilderTaskGroupDialogComponent} from './competition-builder-task-group-dialog/competition-builder-task-group.component';
import {MatChipsModule} from '@angular/material/chips';
import {CompetitionBuilderTaskTypeDialogComponent} from './competition-builder-task-type-dialog/competition-builder-task-type-dialog.component';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatCardModule} from '@angular/material/card';
import {VideoPlayerSegmentBuilderComponent} from './competition-builder-task-dialog/video-player-segment-builder/video-player-segment-builder.component';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {FlexModule} from '@angular/flex-layout';
import {MatSliderModule} from '@angular/material/slider';
import {ServicesModule} from '../../services/services.module';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatGridListModule} from '@angular/material/grid-list';
import {AdvancedBuilderDialogComponent} from './competition-builder-task-dialog/advanced-builder-dialog/advanced-builder-dialog.component';
import {SharedModule} from '../../shared/shared.module';
import {VideoPlayerSegmentBuilderDialogComponent} from './competition-builder-task-dialog/video-player-segment-builder-dialog/video-player-segment-builder-dialog.component';
import {ColorPickerModule} from 'ngx-color-picker';
import {NgxSliderModule} from '@angular-slider/ngx-slider';
import { GeneralCompetitionComponent } from './components/general-competition/general-competition.component';
import { TeamsListComponent } from './components/teams-list/teams-list.component';
import { JudgesListComponent } from './components/judges-list/judges-list.component';
import { TaskTypesComponent } from './components/task-types/task-types.component';
import { TaskGroupsComponent } from './components/task-groups/task-groups.component';
import { TasksTabComponent } from './tabs/tasks-tab/tasks-tab.component';
import { TeamsJudgesTabComponent } from './tabs/teams-judges-tab/teams-judges-tab.component';
import { TypesGroupsTabComponent } from './tabs/types-groups-tab/types-groups-tab.component';
import { GeneralInfoTabComponent } from './tabs/general-info-tab/general-info-tab.component';
import { TabbedBuilderComponent } from './tabbed-builder/tabbed-builder.component';
import {MatTabsModule} from '@angular/material/tabs';

@NgModule({
    imports: [
        MatTableModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatListModule,
        MatProgressSpinnerModule,
        MatMenuModule,
        MatAutocompleteModule,
        MatSelectModule,
        MatChipsModule,
        MatCheckboxModule,
        MatCardModule,
        MatProgressBarModule,
        MatSliderModule,
        MatSlideToggleModule,
        MatButtonToggleModule,
        MatGridListModule,
        NgxSliderModule,
        ServicesModule,
        FlexModule,
        FormsModule,
        ReactiveFormsModule,
        CommonModule,
        SharedModule,
        ColorPickerModule,
        MatTabsModule
    ],
    exports: [CompetitionBuilderComponent],
    declarations: [
        CompetitionBuilderComponent,
        CompetitionBuilderTeamDialogComponent,
        CompetitionBuilderTaskDialogComponent,
        CompetitionBuilderTaskGroupDialogComponent,
        CompetitionBuilderTaskTypeDialogComponent,
        VideoPlayerSegmentBuilderComponent,
        AdvancedBuilderDialogComponent,
        VideoPlayerSegmentBuilderDialogComponent,
        GeneralCompetitionComponent,
        TeamsListComponent,
        JudgesListComponent,
        TaskTypesComponent,
        TaskGroupsComponent,
        TasksTabComponent,
        TeamsJudgesTabComponent,
        TypesGroupsTabComponent,
        GeneralInfoTabComponent,
        TabbedBuilderComponent
    ],
    providers: []
})
export class CompetitionBuilderModule {
}
