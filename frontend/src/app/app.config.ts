import {Injectable} from '@angular/core';
import {IConfig} from './model/config.interface';
import {HttpClient} from '@angular/common/http';

@Injectable()
export class AppConfig {
    static settings: IConfig;
    constructor(private http: HttpClient) {}
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
