import { NgModule } from '@angular/core';

import { CompetitionListComponent } from './competition-list/competition-list.component';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CompetitionCreateDialogComponent } from './competition-list/competition-create-dialog.component';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { CompetitionBuilderModule } from './competition-builder/competition-builder.module';
import { CompetitionStartDialogComponent } from './competition-list/competition-start-dialog.component';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { SharedModule } from '../shared/shared.module';
import {MatChipsModule} from "@angular/material/chips";

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
        MatCheckboxModule,
        SharedModule,
        MatChipsModule,
    ],
  exports: [CompetitionListComponent],
  declarations: [CompetitionListComponent, CompetitionCreateDialogComponent, CompetitionStartDialogComponent],
  providers: [],
})
export class CompetitionModule {}
