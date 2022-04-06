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
import {SharedModule} from '../shared/shared.module';
import {RunAdminSubmissionsListComponent} from './run-admin-submissions-list/run-admin-submissions-list.component';
import {SubmissionOverrideDialogComponent} from './submission-override-dialog/submission-override-dialog.component';
import {ClipboardModule} from '@angular/cdk/clipboard';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {RunScoreHistoryComponent} from './score-history/run-score-history.component';
import {NgApexchartsModule} from 'ng-apexcharts';
import {MatPaginatorModule} from '@angular/material/paginator';
import {MatExpansionModule} from '@angular/material/expansion';
import {RunAsyncAdminViewComponent} from './run-async-admin-view/run-async-admin-view.component';
import {FlexModule} from '@angular/flex-layout';
import {MatToolbarModule} from '@angular/material/toolbar';

@NgModule({
    imports: [
        MatTableModule,
        MatPaginatorModule,
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
        MatCardModule,
        SharedModule,
        ClipboardModule,
        MatButtonToggleModule,
        MatSlideToggleModule,
        NgApexchartsModule,
        MatExpansionModule,
        FlexModule,
        MatToolbarModule
    ],
    exports: [RunListComponent, RunAdminViewComponent, RunScoreHistoryComponent],
    declarations: [RunListComponent, RunAdminViewComponent, RunScoreHistoryComponent, AdminRunListComponent, ViewerRunListComponent, RunAdminSubmissionsListComponent, SubmissionOverrideDialogComponent, RunAsyncAdminViewComponent],
    providers: []
})
export class RunModule {
}
