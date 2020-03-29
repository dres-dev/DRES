import {NgModule} from '@angular/core';
import {CompetitionBuilerComponent} from './competition-builer/competition-builer.component';
import {CompetitionListComponent} from './competition-list/competition-list.component';
import {MatTableModule} from '@angular/material/table';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatTooltipModule} from '@angular/material/tooltip';
import {CompetitionCreateDialogComponent} from './competition-list/competition-create-dialog.component';
import {MatDialogModule} from '@angular/material/dialog';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {CompetitionBuilderAddTeamDialogComponent} from './competition-builer/competition-builder-add-team-dialog.component';
import {MatListModule} from '@angular/material/list';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatMenuModule} from '@angular/material/menu';

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
        MatMenuModule
    ],
    exports:      [ CompetitionBuilerComponent, CompetitionListComponent ],
    declarations: [ CompetitionBuilerComponent, CompetitionListComponent, CompetitionCreateDialogComponent, CompetitionBuilderAddTeamDialogComponent ],
    providers:    [ ]
})
export class CompetitionModule { }
