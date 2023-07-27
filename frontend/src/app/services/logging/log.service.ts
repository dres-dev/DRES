import { Inject, Injectable, isDevMode } from "@angular/core";
import { LogEntry } from "./log-entry.model";
import { LogLevel } from "./log-level.enum";
import { LOGGER_CONFIG } from "./logger-config.token";
import { LoggerConfig } from "./logger.config";

@Injectable({providedIn: 'root'})
export class LogService {

  private readonly level: LogLevel = isDevMode() ? LogLevel.ALL : LogLevel.INFO

  private readonly source;

  static callingTrace(){
    const e = new Error();
    const regex = /\((.*):(\d+):(\d+)\)$/
    const match = regex.exec(e.stack.split("\n")[2]);
    return {
      filepath: match[1],
      line: match[2],
      column: match[3]
    };
  }

  constructor(@Inject(LOGGER_CONFIG) config: LoggerConfig) {
    this.source = config.identifier;
  }

  public trace(msg: string, ...data: any[]){
    this.log({ source: this.source, timestamp: new Date(), level: LogLevel.TRACE, message: msg, data: data})
  }
  public debug(msg: string, ...data: any[]){
    this.log({ source: this.source, timestamp: new Date(), level: LogLevel.DEBUG, message: msg, data: data})
  }

  public info(msg: string, ...data: any[]){
    this.log({ source: this.source, timestamp: new Date(), level: LogLevel.INFO, message: msg, data: data})
  }

  public warn(msg: string, ...data: any[]){
    this.log({ source: this.source, timestamp: new Date(), level: LogLevel.WARN, message: msg, data: data})
  }

  public error(msg: string, ...data: any[]){
    this.log({ source: this.source, timestamp: new Date(), level: LogLevel.ERROR, message: msg, data: data})
  }

  public fatal(msg: string, ...data: any[]){
    this.log({ source: this.source, timestamp: new Date(), level: LogLevel.FATAL, message: msg, data: data})
  }

  private log(log: LogEntry){
    if(!this.checkLogLevel(log.level) || (this.level === LogLevel.OFF)){
      return;
    }
    /* We have FATAl but console doesnt */
    const lvl = (log.level === LogLevel.FATAL ? 'ERROR' : LogLevel[log.level]).toLowerCase()
    const prefix = `${log.timestamp.toISOString()} [${log.source}] ${lvl}:`
    if(log?.data?.length > 0){
      console[lvl](prefix, log.message, ...log.data)
    }else{
      console[lvl](prefix, log.message)
    }
  }

  private checkLogLevel(level: LogLevel){
    return level >= this.level;
  }
}
