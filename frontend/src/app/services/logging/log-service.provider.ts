import { LogService } from "./log.service";
import { logServiceFactory } from "./log-service.factory";
import { LOGGER_CONFIG } from "./logger-config.token";

export const logServiceProvider = {
  provide: LogService,
  useFactory: logServiceFactory,
  deps: [LOGGER_CONFIG],
};
