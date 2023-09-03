import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { TemplateBuilderComponent } from "./template-builder.component";
import { TemplateBuilderComponentsModule } from "./components/template-builder-components.module";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from "@angular/material/tabs";
import { SharedModule } from "../../shared/shared.module";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatInputModule } from "@angular/material/input";
import { ReactiveFormsModule } from "@angular/forms";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatSelectModule } from "@angular/material/select";
import { CompetitionBuilderModule } from "../../competition/competition-builder/competition-builder.module";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { TeamgroupsListComponent } from './components/teamgroups-list/teamgroups-list.component';
import { TeamgroupsDialogComponent } from './components/teamgroups-dialog/teamgroups-dialog.component';
import { MatDialogModule } from "@angular/material/dialog";
import { MatChipsModule } from "@angular/material/chips";
import { TemplateImportTreeComponent } from './components/template-import-tree/template-import-tree.component';
import { MatTreeModule } from "@angular/material/tree";
import { MatCheckboxModule } from "@angular/material/checkbox";


@NgModule({
  declarations: [
    TemplateBuilderComponent,
    TeamgroupsListComponent,
    TeamgroupsDialogComponent,
    TemplateImportTreeComponent

  ],
  imports: [
    CommonModule,
    MatIconModule,
    MatTabsModule,
    SharedModule,
    MatTooltipModule,
    MatInputModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatSelectModule,
    CompetitionBuilderModule,
    TemplateBuilderComponentsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatChipsModule,
    MatTreeModule,
    MatCheckboxModule
  ],
  exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {
}
