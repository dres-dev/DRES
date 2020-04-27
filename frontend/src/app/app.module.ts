import {BrowserModule} from '@angular/platform-browser';
import {APP_INITIALIZER, NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {LoginComponent} from './user/login/login.component';
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


/**
 * Method used to load application config.
 *
 * @param appConfig AppConfig service
 */
export function initializeApp(appConfig: AppConfig) {
    return () => appConfig.load();
}

@NgModule({
  declarations: [AppComponent],
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
        CompetitionModule,
        ViewerModule,
        RunModule,
        JudgementModule
    ],
  providers: [
      AppConfig,
      {provide: APP_INITIALIZER, useFactory: initializeApp, deps: [AppConfig], multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
