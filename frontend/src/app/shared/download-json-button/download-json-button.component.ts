import {Component, Input, ViewChild} from '@angular/core';
import {MatButton} from '@angular/material/button';

@Component({
    selector: 'app-download-json-button',
    templateUrl: './download-json-button.component.html',
    styleUrls: ['./download-json-button.component.scss']
})
export class DownloadJsonButtonComponent {

    /**
     * The provider for the downloadable content
     */
    @Input() downloadable: () => any;
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
    @ViewChild('dwnld-btn', {static: false}) btn: MatButton;

    @Input() name = 'Download';

    public download() {
        const file = new Blob([this.downloadable()], {type: this.contentType});
        const fake = document.createElement('a');
        fake.href = URL.createObjectURL(file);
        fake.download = this.fileName ? this.fileName() : 'download.json';
        fake.click();
        URL.revokeObjectURL(fake.href);
    }

}
