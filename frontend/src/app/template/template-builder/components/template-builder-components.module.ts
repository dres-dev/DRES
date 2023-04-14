import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TemplateInformationComponent} from './template-information/template-information.component';
import {ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import { JudgesListComponent } from './judges-list/judges-list.component';
import {MatButtonModule} from '@angular/material/button';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatTableModule} from '@angular/material/table';
import { TeamsListComponent } from './teams-list/teams-list.component';
import { TaskTypesListComponent } from './task-types-list/task-types-list.component';
import { MatListModule } from "@angular/material/list";
import { TaskGroupsListComponent } from './task-groups-list/task-groups-list.component';
import { SharedModule } from "../../../shared/shared.module";
import { TaskTemplatesListComponent } from './tasks-list/task-templates-list.component';
import { TaskTemplateEditorComponent } from './task-template-editor/task-template-editor.component';
import { MatSelectModule } from "@angular/material/select";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatGridListModule } from "@angular/material/grid-list";
import { CompetitionBuilderModule } from "../../../competition/competition-builder/competition-builder.module";


@NgModule({
    declarations: [
        TemplateInformationComponent,
        JudgesListComponent,
        TeamsListComponent,
        TaskTypesListComponent,
        TaskGroupsListComponent,
        TaskTemplatesListComponent,
        TaskTemplateEditorComponent
    ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTooltipModule,
    MatIconModule,
    MatMenuModule,
    MatAutocompleteModule,
    MatTableModule,
    MatListModule,
    SharedModule,
    MatSelectModule,
    MatButtonToggleModule,
    MatGridListModule,
    CompetitionBuilderModule
  ],
  exports: [TemplateInformationComponent, JudgesListComponent, TeamsListComponent, TaskTypesListComponent, TaskGroupsListComponent, TaskTemplatesListComponent, TaskTemplateEditorComponent]
})
export class TemplateBuilderComponentsModule {
}
