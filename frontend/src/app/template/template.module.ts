import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatInputModule} from '@angular/material/input';
import {ReactiveFormsModule} from '@angular/forms';
import {TemplateBuilderModule} from './template-builder/template-builder.module';
import { TemplateListComponent } from './template-list/template-list.component';
import { TemplateCreateDialogComponent } from './template-create-dialog/template-create-dialog.component';
import { MatButtonModule } from "@angular/material/button";
import { MatDialogModule } from "@angular/material/dialog";
import { EvaluationStartDialogComponent } from './evaluation-start-dialog/evaluation-start-dialog.component';
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatOptionModule } from "@angular/material/core";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { SharedModule } from "../shared/shared.module";
import { MatTableModule } from "@angular/material/table";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";


@NgModule({
    declarations: [
    TemplateListComponent,
    TemplateCreateDialogComponent,
    EvaluationStartDialogComponent
  ],
  imports: [
    CommonModule,
    MatInputModule,
    ReactiveFormsModule,
    TemplateBuilderModule,
    MatButtonModule,
    MatDialogModule,
    MatCheckboxModule,
    MatOptionModule,
    MatSelectModule,
    MatTooltipModule,
    SharedModule,
    MatTableModule,
    MatProgressSpinnerModule
  ]
})
export class TemplateModule {
}
