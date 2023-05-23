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
import { EnhanceTaskSubmissionInfoPipe } from './pipes/enhance-task-submission-info.pipe';
import {ApiModule, Configuration} from '../../../openapi';
import { SpaceToNewlinePipe } from './pipes/space-to-newline.pipe';
import { FormatTemporalUnitPipe } from './pipes/format-temporal-unit.pipe';
import { FormatTemporalPointPipe } from './pipes/format-temporal-point.pipe';
import { ResolveMediaItemPipe } from './pipes/resolve-mediaitem.pipe';
import { ResolveMediaItemUrlPipe } from './pipes/resolve-media-item-url.pipe';
import { ResolveMediaItemPreviewPipe } from './pipes/resolve-media-item-preview.pipe';
import { FormatMediaItemPipe } from './pipes/format-media-item.pipe';

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
    EnhanceTaskSubmissionInfoPipe,
    SpaceToNewlinePipe,
    FormatTemporalPointPipe,
    ResolveMediaItemPipe,
    ResolveMediaItemUrlPipe,
    ResolveMediaItemPreviewPipe
  ],
  declarations: [
    RoundPipePipe,
    FormatTimePipePipe,
    BackButtonDirective,
    ForwardButtonDirective,
    Epoch2DatePipePipe,
    EnhanceTaskPastInfoPipe,
    ResolveTeamPipe,
    EnhanceTaskSubmissionInfoPipe,
    SpaceToNewlinePipe,
    FormatTemporalUnitPipe,
    FormatTemporalPointPipe,
    ResolveMediaItemPipe,
    ResolveMediaItemUrlPipe,
    ResolveMediaItemPreviewPipe,
    FormatMediaItemPipe,
  ],
  providers: [
    AuthenticationService,
    NavigationService,
    CanDeactivateGuard,
    FormatTemporalPointPipe,
    FormatTemporalUnitPipe,
    FormatTimePipePipe,
    Epoch2DatePipePipe,
    FormatMediaItemPipe],
})
export class ServicesModule {}
