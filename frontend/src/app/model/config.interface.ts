export interface IConfig {
  endpoint: { host: string; port: number; tls: boolean };
  effects: { mute: boolean };
}
