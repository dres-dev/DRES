import {BrowserModule} from '@angular/platform-browser';
import {APP_INITIALIZER, NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatCardModule} from '@angular/material/card';
import {MatInputModule} from '@angular/material/input';
import {HttpClientModule} from '@angular/common/http';
import {ServicesModule} from './services/services.module';
import {MatMenuModule} from '@angular/material/menu';
import {CompetitionModule} from './competition/competition.module';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {RunModule} from './run/run.module';
import {ViewerModule} from './viewer/viewer.module';
import {AppConfig} from './app.config';
import {UserModule} from './user/user.module';
import {JudgementModule} from './judgement/judgement.module';
import {AccessRoleService} from './services/session/access-role.service';
import {CollectionListComponent} from './collection/collection-list/collection-list.component';
import {MatListModule} from '@angular/material/list';
import {MatTableModule} from '@angular/material/table';
import {CollectionViewerComponent} from './collection/collection-viewer/collection-viewer.component';
import {MediaItemBuilderDialogComponent} from './collection/collection-builder/media-item-builder-dialog/media-item-builder-dialog.component';
import {CollectionBuilderDialogComponent} from './collection/collection-builder/collection-builder-dialog/collection-builder-dialog.component';
import {MatDialogModule} from '@angular/material/dialog';
import {MatSelectModule} from '@angular/material/select';
import {MatPaginatorModule} from '@angular/material/paginator';
import {AuditlogModule} from './auditlog/auditlog.module';
import {SharedModule} from './shared/shared.module';


/**
 * Method used to load application config.
 *
 * @param appConfig AppConfig service
 */
export function initializeApp(appConfig: AppConfig) {
    return () => appConfig.load();
}

@NgModule({
    declarations: [AppComponent, CollectionListComponent, CollectionViewerComponent, MediaItemBuilderDialogComponent, CollectionBuilderDialogComponent],
    imports: [
        BrowserModule,
        AppRoutingModule,
        BrowserAnimationsModule,
        MatToolbarModule,
        MatIconModule,
        MatButtonModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatCardModule,
        MatInputModule,
        MatSnackBarModule,
        MatMenuModule,
        MatTooltipModule,

        HttpClientModule,

        ServicesModule,
        UserModule,
        AuditlogModule,
        CompetitionModule,
        ViewerModule,
        RunModule,
        JudgementModule,
        MatListModule,
        MatTableModule,
        MatDialogModule,
        MatSelectModule,
        MatPaginatorModule,
        SharedModule
    ],
    providers: [
        AppConfig,
        {provide: APP_INITIALIZER, useFactory: initializeApp, deps: [AppConfig], multi: true},
        AccessRoleService
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
