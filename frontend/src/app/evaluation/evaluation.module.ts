import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnswerComponent } from './admin/submission/answer/answer.component';
import { MatTableModule } from "@angular/material/table";
import { SharedModule } from "../shared/shared.module";
import { AnswerSetComponent } from './admin/submission/answer-set/answer-set.component';
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatTooltipModule } from "@angular/material/tooltip";
import { SubmissionsDetailsComponent } from './admin/submission/submissions-details/submissions-details.component';
import { SubmissionsListComponent } from './admin/submission/submissions-list/submissions-list.component';
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatExpansionModule } from "@angular/material/expansion";
import { FormsModule } from "@angular/forms";
import { MatCardModule } from "@angular/material/card";



@NgModule({
  declarations: [
    AnswerComponent,
    AnswerSetComponent,
    SubmissionsDetailsComponent,
    SubmissionsListComponent,
  ],
  imports: [
    CommonModule,
    MatTableModule,
    SharedModule,
    MatButtonToggleModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatExpansionModule,
    FormsModule,
    MatCardModule
  ]
})
export class EvaluationModule { }
