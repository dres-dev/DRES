import {Injectable} from '@angular/core';
import {IConfig} from './model/config.interface';
import {HttpClient} from '@angular/common/http';
import {DefaultConfig} from './model/config.default';
import {BehaviorSubject, Observable} from 'rxjs';

@Injectable()
export class AppConfig {


    private static settingsSubject = new BehaviorSubject<IConfig>(null);
    static settings: IConfig;
    private handler: ProxyHandler<IConfig> = {
        set: (obj: IConfig, prop, value: any) => {
            obj[prop] = value;
            AppConfig.settingsSubject.next(AppConfig.settings);
            return true;
        }
    } as ProxyHandler<IConfig>;

    constructor(private http: HttpClient) {}

    /**
     * Returns the current configuration
     */
    get config(): IConfig {
        return AppConfig.settings;
    }

    /**
     * Returns the current configuration as observable. Can be used to monitor changes.
     */
    get configAsObservable(): Observable<IConfig> {
        return AppConfig.settingsSubject.asObservable();
    }

    /**
     * Returns the URL to the DRES WebSocket endpoint.
     */
    get webSocketUrl() {
       return  `${AppConfig.settings.endpoint.tls ? 'wss://' : 'ws://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}/api/ws/run`;
    }

    /**
     * Resolves the given path given the global API URL. That is, it returns the URL of the
     * form http://[host]:[port]/api/[path]
     *
     * @param path The path to resolve.
     * @return The full URL to the API endpoint.
     */
    public resolveApiUrl(path: string) {
        return `${AppConfig.settings.endpoint.tls ? 'https://' : 'http://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}/api${path.startsWith('/') ? '' : '/'}${path}`;
    }

    public resolveUrl(path: string) {
        return `${AppConfig.settings.endpoint.tls ? 'https://' : 'http://'}${AppConfig.settings.endpoint.host}:${AppConfig.settings.endpoint.port}${path.startsWith('/') ? '' : '/'}${path}`;
    }

    /**
     * Loads the default configuration from a JSON file.
     */
    load() {
        const jsonFile = 'config.json?random=' + Date.now();
        return new Promise<void>((resolve, reject) => {
            this.http.get(jsonFile).toPromise().then((response: IConfig) => {
                AppConfig.settings = new Proxy<IConfig>(response as IConfig, this.handler);
                AppConfig.settingsSubject.next(AppConfig.settings);
                resolve();
            }).catch((response: any) => {
                AppConfig.settings = new Proxy<IConfig>(new DefaultConfig(), this.handler);
                AppConfig.settingsSubject.next(AppConfig.settings);
                console.log(`Could not load config file '${jsonFile}'. Fallback to default.`);
                resolve();
            });
        });
    }
}
