import { LogLevel } from "./log-level.enum";

export interface LogEntry {
  timestamp: Date
  level: LogLevel
  source: string
  message: string
  data?: any[]
}
