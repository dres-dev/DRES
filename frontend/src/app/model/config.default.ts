import {IConfig} from './config.interface';

export class DefaultConfig implements IConfig {
    private url = new URL(window.location.href);
    endpoint =  {
        host: this.url.hostname,
        port: parseInt(this.url.port, 10),
        tls: this.url.protocol === 'https:'
    };
}
