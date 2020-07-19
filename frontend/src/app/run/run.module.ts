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
import {RunListComponent} from './run-list.component';
import {AdminRunListComponent} from './admin-run-list.component';
import {ViewerRunListComponent} from './viewer-run-list.component';
import {RunAdminViewComponent} from './run-admin-view.component';
import {MatCardModule} from '@angular/material/card';

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
        MatCardModule
    ],
    exports:      [ RunListComponent, RunAdminViewComponent ],
    declarations: [ RunListComponent, RunAdminViewComponent, AdminRunListComponent, ViewerRunListComponent ],
    providers:    [ ]
})
export class RunModule { }
