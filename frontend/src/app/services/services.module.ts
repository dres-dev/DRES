import {ApiModule, Configuration} from '../../../openapi';
import {NgModule} from '@angular/core';
import {AppConfig} from '../app.config';
import {AuthenticationService} from './session/authentication.sevice';
import { RoundPipePipe } from './pipes/round-pipe.pipe';
import { FormatTimePipePipe } from './pipes/format-time-pipe.pipe';
import {NavigationService} from './navigation/navigation.service';
import { BackButtonDirective } from './navigation/back-button.directive';
import { ForwardButtonDirective } from './navigation/forward-button.directive';
import { Epoch2DatePipePipe } from './pipes/epoch2date.pipe';
import {CanDeactivateGuard} from './can-deactivate.guard';

@NgModule({
  imports: [ApiModule.forRoot(() => {
    return new Configuration({
      basePath: `${AppConfig.settings.endpoint.tls ? 'https://' : 'http://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}`,
      withCredentials: true
    });
  })],
  exports: [ApiModule, RoundPipePipe, FormatTimePipePipe, BackButtonDirective, ForwardButtonDirective, Epoch2DatePipePipe],
  declarations: [RoundPipePipe, FormatTimePipePipe, BackButtonDirective, ForwardButtonDirective, Epoch2DatePipePipe],
  providers: [AuthenticationService, NavigationService, CanDeactivateGuard]
})
export class ServicesModule {}
