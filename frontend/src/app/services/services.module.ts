import {ApiModule, Configuration} from '../../../openapi';
import {NgModule} from '@angular/core';
import {AppConfig} from '../app.config';

@NgModule({
  imports: [ApiModule.forRoot(() => {
    return new Configuration({
      basePath: `${AppConfig.settings.endpoint.tls ? 'https://' : 'http://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}`,
      withCredentials: true
    });
  })],
  exports: [ApiModule],
  declarations: [],
  providers: []
})
export class ServicesModule {}
