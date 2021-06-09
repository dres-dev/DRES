import {IConfig} from './config.interface';

export class DefaultConfig implements IConfig {
    private url = new URL(window.location.href);
    public readonly endpoint =  {
        host: this.url.hostname,
        port: this.url.port === '' ? -1 : parseInt(this.url.port, 10),
        tls: this.url.protocol === 'https:'
    };
    public readonly effects = {
        mute: false
    };
}
