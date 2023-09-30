import { Component, Input, ViewChild } from '@angular/core';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'app-upload-json-button',
  templateUrl: './upload-json-button.component.html',
  styleUrls: ['./upload-json-button.component.scss'],
})
export class UploadJsonButtonComponent {

  @Input() inline = false;
  /** The display name for the button. Defaults to 'Upload' */
  @Input() name = 'Upload';
  /** If multi-select files are enabled. Defaults to false (only single file) */
  @Input() multi = false; // Currently only single file upload handled
  /** The handler to process the uploaded thing */
  @Input() handler: (contents: string) => void;

  @ViewChild('fileInput', { static: false }) fileUpload: HTMLInputElement;

  constructor() {}

  /*onClick(){
        this.fileUpload.onchange = this.onFileSelected;
        this.fileUpload.click();
    }*/

  onFileSelected() {
    if (this.handler) {
      /* We only do stuff when there is a handler */
      const inputNode: any = document.querySelector('#fileInput');

      if (typeof FileReader !== 'undefined') {
        const reader = new FileReader();

        reader.onload = (e: any) => {
          this.handler(e.target.result);
        };
        if (this.multi) {
          // TODO multi file upload
          console.error('UploadJsonButton: Cannot handle multi-select');
        } else {
          reader.readAsText(inputNode.files[0]);
        }
      }
    }
  }
}
