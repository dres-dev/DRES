import { ApiModule, Configuration } from '../../../openapi';
import { NgModule } from '@angular/core';
import { AppConfig } from '../app.config';
import { AuthenticationService } from './session/authentication.sevice';
import { RoundPipePipe } from './pipes/round-pipe.pipe';
import { FormatTimePipePipe } from './pipes/format-time-pipe.pipe';
import { NavigationService } from './navigation/navigation.service';
import { BackButtonDirective } from './navigation/back-button.directive';
import { ForwardButtonDirective } from './navigation/forward-button.directive';
import { Epoch2DatePipePipe } from './pipes/epoch2date.pipe';
import { CanDeactivateGuard } from './can-deactivate.guard';
import { EnhanceTaskPastInfoPipe } from './pipes/enhance-task-past-info.pipe';
import { ResolveTeamPipe } from './pipes/resolve-team.pipe';

/**
 * Provides the {@link AppConfig} reference.
 *
 * @param appConfig Reference (provided by DI).
 */
export function initializeApiConfig(appConfig: AppConfig) {
  return new Configuration({ basePath: appConfig.baseUrl, withCredentials: true });
}

@NgModule({
  imports: [
    {
      ngModule: ApiModule,
      providers: [{ provide: Configuration, useFactory: initializeApiConfig, deps: [AppConfig] }],
    },
  ],
  exports: [
    ApiModule,
    RoundPipePipe,
    FormatTimePipePipe,
    BackButtonDirective,
    ForwardButtonDirective,
    Epoch2DatePipePipe,
    EnhanceTaskPastInfoPipe,
    ResolveTeamPipe,
  ],
  declarations: [
    RoundPipePipe,
    FormatTimePipePipe,
    BackButtonDirective,
    ForwardButtonDirective,
    Epoch2DatePipePipe,
    EnhanceTaskPastInfoPipe,
    ResolveTeamPipe,
  ],
  providers: [AuthenticationService, NavigationService, CanDeactivateGuard],
})
export class ServicesModule {}
