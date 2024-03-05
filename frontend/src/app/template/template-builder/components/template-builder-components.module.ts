import { NgModule } from "@angular/core";
import {CommonModule, NgOptimizedImage} from "@angular/common";
import { TemplateInformationComponent } from "./template-information/template-information.component";
import { ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { JudgesListComponent } from "./judges-list/judges-list.component";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatTableModule } from "@angular/material/table";
import { TeamsListComponent } from "./teams-list/teams-list.component";
import { TaskTypesListComponent } from "./task-types-list/task-types-list.component";
import { MatListModule } from "@angular/material/list";
import { TaskGroupsListComponent } from "./task-groups-list/task-groups-list.component";
import { SharedModule } from "../../../shared/shared.module";
import { TaskTemplatesListComponent } from "./tasks-list/task-templates-list.component";
import { TaskTemplateEditorComponent } from "./task-template-editor/task-template-editor.component";
import { MatSelectModule } from "@angular/material/select";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatGridListModule } from "@angular/material/grid-list";
import { CompetitionBuilderModule } from "../../../competition/competition-builder/competition-builder.module";
import { QueryDescriptionFormFieldComponent } from "./query-description-form-field/query-description-form-field.component";
import { QueryDescriptionTextFormFieldComponent } from "./query-description-text-form-field/query-description-text-form-field.component";
import {
  QueryDescriptionExternalFormFieldComponent
} from "./query-description-external-form-field/query-description-external-form-field.component";
import {
  QueryDescriptionMediaItemFormFieldComponent
} from "./query-description-media-item-form-field/query-description-media-item-form-field.component";
import {
  QueryDescriptionMediaItemImageFormFieldComponent
} from "./query-description-media-item-image-form-field/query-description-media-item-image-form-field.component";
import {
  QueryDescriptionMediaItemVideoFormFieldComponent
} from "./query-description-media-item-video-form-field/query-description-media-item-video-form-field.component";
import {
  QueryDescriptionExternalVideoFormFieldComponent
} from "./query-description-external-video-form-field/query-description-external-video-form-field.component";
import {
  QueryDescriptionExternalImageFormFieldComponent
} from "./query-description-external-image-form-field/query-description-external-image-form-field.component";
import { TeamBuilderDialogComponent } from './team-builder-dialog/team-builder-dialog.component';
import { MatChipsModule } from "@angular/material/chips";
import { MatDialogModule } from "@angular/material/dialog";
import { ColorPickerModule } from "ngx-color-picker";
import {CdkDrag, CdkDropList} from '@angular/cdk/drag-drop';
import { MatCardModule } from "@angular/material/card";
import { ViewersListComponent } from './viewers-list/viewers-list.component';


@NgModule({
  declarations: [
    TemplateInformationComponent,
    JudgesListComponent,
    TeamsListComponent,
    TaskTypesListComponent,
    TaskGroupsListComponent,
    TaskTemplatesListComponent,
    TaskTemplateEditorComponent,
    QueryDescriptionFormFieldComponent,
    QueryDescriptionTextFormFieldComponent,
    QueryDescriptionExternalFormFieldComponent,
    QueryDescriptionMediaItemFormFieldComponent,
    QueryDescriptionMediaItemImageFormFieldComponent,
    QueryDescriptionMediaItemVideoFormFieldComponent,
    QueryDescriptionExternalVideoFormFieldComponent,
    QueryDescriptionExternalImageFormFieldComponent,
    TeamBuilderDialogComponent,
    ViewersListComponent
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
    CompetitionBuilderModule,
    NgOptimizedImage,
    MatChipsModule,
    MatDialogModule,
    ColorPickerModule,
    CdkDropList,
    CdkDrag,
    MatCardModule
  ],
  exports: [TemplateInformationComponent,
    JudgesListComponent,
    TeamsListComponent,
    TaskTypesListComponent,
    TaskGroupsListComponent,
    TaskTemplatesListComponent,
    TaskTemplateEditorComponent, ViewersListComponent]
})
export class TemplateBuilderComponentsModule {
}
