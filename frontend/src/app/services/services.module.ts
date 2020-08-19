import {ApiModule, Configuration} from '../../../openapi';
import {NgModule} from '@angular/core';
import {AppConfig} from '../app.config';
import {AuthenticationService} from './session/authentication.sevice';
import { RoundPipePipe } from './pipes/round-pipe.pipe';
import { FormatTimePipePipe } from './pipes/format-time-pipe.pipe';

@NgModule({
  imports: [ApiModule.forRoot(() => {
    return new Configuration({
      basePath: `${AppConfig.settings.endpoint.tls ? 'https://' : 'http://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}`,
      withCredentials: true
    });
  })],
  exports: [ApiModule, RoundPipePipe, FormatTimePipePipe],
  declarations: [RoundPipePipe, FormatTimePipePipe],
  providers: [AuthenticationService]
})
export class ServicesModule {}
