import { InjectionToken } from "@angular/core";
import { LoggerConfig } from "./logger.config";

export const LOGGER_CONFIG = new InjectionToken<LoggerConfig>("__LOGGER_CONFIG__", {
  providedIn: "root",
  factory: () => {
    return {identifier: "Root"} as LoggerConfig
  }
});
