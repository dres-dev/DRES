import { Injectable } from '@angular/core';
import { IConfig } from './model/config.interface';
import { HttpClient } from '@angular/common/http';
import { DefaultConfig } from './model/config.default';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable()
export class AppConfig {
  /** BehaviorSubject used to cache the current {IConfig} */
  private settings = new BehaviorSubject<IConfig>(null);

  constructor(private http: HttpClient) {}

  /**
   * Returns the current configuration
   */
  get config(): IConfig {
    return this.settings.value;
  }

  /**
   * Returns the current configuration as observable. Can be used to monitor changes.
   */
  get configAsObservable(): Observable<IConfig> {
    return this.settings.asObservable();
  }

  /**
   * Base URL for DRES HTTP endpoint.
   */
  get baseUrl() {
    const config = this.config;
    const port = config.endpoint.port === -1 ? '' : `:${config.endpoint.port}`;
    return `${config.endpoint.tls ? 'https://' : 'http://'}${config.endpoint.host}${port}`;
  }

  /**
   * URL to the DRES WebSocket endpoint.
   */
  get webSocketUrl() {
    const config = this.config;
    const port = config.endpoint.port === -1 ? '' : `:${config.endpoint.port}`;
    return `${config.endpoint.tls ? 'wss://' : 'ws://'}${config.endpoint.host}${port}/api/ws/run`;
  }

  /**
   * Resolves the given path given the global backend URL.
   *
   * That is, it returns the URL of the form http://[host]:[port]/[path]
   *
   * @param path The path to resolve.
   * @return The full URL to the API endpoint.
   */
  public resolveUrl(path: string) {
    return `${this.baseUrl}${path.startsWith('/') ? '' : '/'}${path}`;
  }

  /**
   * Resolves the given path given the global API URL. That is, it returns the URL of the
   * form http://[host]:[port]/api/[path]
   *
   * @param path The path to resolve.
   * @return The full URL to the API endpoint.
   */
  public resolveApiUrl(path: string, version: string = 'v2') {
    return `${this.baseUrl}/api/${version}${path.startsWith('/') ? '' : '/'}${path}`;
  }

  /**
   * Resolves the given media item's url so to fetch the actual content. Provides a URL compliant with the GetMediaHandler (backend)
   * @param mediaItemId The media item's id to resolve the url for
   * @param version The API version
   */
  public resolveMediaItemUrl(mediaItemId: string, version: string = "v2"): string{
    return this.resolveApiUrl(`/media/${mediaItemId}`, version);
  }

  /**
   * Resolves the given media item's PREVIEW url so to fetch the actual content. Provides a URL compliant with the PreviewImageHandler (backend)
   * @param mediaItemId The media item's id to resolve the PREVIEW url for
   * @param version The API version
   */
  public resolveImagePreviewUrl(mediaItemId: string, timeInMs?:string,version: string = "v2"): string{
    return this.resolveApiUrl(`/preview/${mediaItemId}${timeInMs ? `/${timeInMs}` : ''}`, version)
  }

  /**
   * Resolves the given external file URL. Provides a URL compliant with the GetExternalMediaHandler (backend)
   * @param file The file including extension to resolve the URL for
   * @param version The API version
   */
  public resolveExternalUrl(file: string, version: string= "v2"): string {
    return this.resolveApiUrl(`/media/external/${file}`, version)
  }



  /**
   * (Re-)loads the default configuration from a JSON file.
   */
  public load() {
    const jsonFile = 'config.json?random=' + Date.now();
    return new Promise<void>((resolve, reject) => {
      this.http
        .get(jsonFile)
        .toPromise()
        .then((response: IConfig) => {
          this.settings.next(response as IConfig);
          resolve();
        })
        .catch((response: any) => {
          this.settings.next(new DefaultConfig());
          console.log(`Could not load config file '${jsonFile}'. Fallback to default.`);
          resolve();
        });
    });
  }
}
