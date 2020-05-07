import {Injectable} from '@angular/core';
import {IConfig} from './model/config.interface';
import {HttpClient} from '@angular/common/http';

@Injectable()
export class AppConfig {
    static settings: IConfig;
    constructor(private http: HttpClient) {}

    /**
     * Returns the IConfig object for this application
     *
     * @return Current IConfig instance.
     */
    get config(): IConfig {
        return AppConfig.settings;
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

    load() {
        const jsonFile = 'config.json';
        return new Promise<void>((resolve, reject) => {
            this.http.get(jsonFile).toPromise().then((response: IConfig) => {
                AppConfig.settings = response as IConfig;
                resolve();
            }).catch((response: any) => {
                reject(`Could not load config file '${jsonFile}': ${JSON.stringify(response)}`);
            });
        });
    }
}
