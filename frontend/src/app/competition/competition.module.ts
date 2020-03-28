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
import {ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';

@NgModule({
    imports: [
        MatTableModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        ReactiveFormsModule,
        CommonModule
    ],
    exports:      [ CompetitionBuilerComponent, CompetitionListComponent ],
    declarations: [ CompetitionBuilerComponent, CompetitionListComponent, CompetitionCreateDialogComponent ],
    providers:    [ ]
})
export class CompetitionModule { }
