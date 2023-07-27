import { LogService } from "./log.service";
import { LoggerConfig } from "./logger.config";

export const logServiceFactory = (config: LoggerConfig) => {
  return new LogService(config)
}
