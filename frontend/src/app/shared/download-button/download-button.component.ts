import { Component, Input, ViewChild } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-download-button',
  templateUrl: './download-button.component.html',
  styleUrls: ['./download-button.component.scss'],
})
export class DownloadButtonComponent {
  /**
   * The provider for the downloadable content
   */
  @Input() downloadable: () => any;

  /**
   * The provider for the downloadable content as a promise
   */
  @Input() downloadProvider: Observable<any>;

  /**
   * The content type. Defaults to json
   */
  @Input() contentType = 'application/json'; // Or text/plain ?
  /**
   * The provider for ther file name
   */
  @Input() fileName: () => string;

  /**
   * The button component
   */
  @ViewChild('dwnld-btn', { static: false }) btn: MatButton;

  @Input() name = '';

  @Input() icon = 'cloud_download';

  @Input() inline = false;

  public download() {
    if (typeof this.downloadable === 'function') {
      this.doDownload(this.downloadable());
    } else if (typeof this.downloadProvider === 'object') {
      this.downloadProvider.subscribe((data) => {
        if (this.contentType === 'application/json') {
          this.doDownload(JSON.stringify(data, null, ' '));
        } else {
          this.doDownload(data);
        }
      });
    } else {
      console.error('Cannot download as no provider was given. This is a developer error.');
    }
  }

  private doDownload(downloadable: any) {
    const file = new Blob([downloadable], { type: this.contentType });
    const fake = document.createElement('a');
    fake.href = URL.createObjectURL(file);
    fake.download = this.fileName ? this.fileName() : 'download.json';
    fake.click();
    URL.revokeObjectURL(fake.href);
  }
}
