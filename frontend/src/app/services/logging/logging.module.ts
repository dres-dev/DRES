import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { LogService } from "./log.service";
import { LOGGER_CONFIG } from "./logger-config.token";
import { LoggerConfig } from "./logger.config";
import { logServiceProvider } from "./log-service.provider";


@NgModule({
  declarations: [],
  imports: [
    CommonModule
  ], providers: [
    LogService,
    logServiceProvider
  ]
})
export class LoggingModule {
}
