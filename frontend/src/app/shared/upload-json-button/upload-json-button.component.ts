import { Component, ElementRef, Input, ViewChild } from '@angular/core';

@Component({
  selector: 'app-upload-json-button',
  templateUrl: './upload-json-button.component.html',
  styleUrls: ['./upload-json-button.component.scss'],
  standalone: false,
})
export class UploadJsonButtonComponent {
  @Input() inline = false;
  /** The display name for the button. Defaults to 'Upload' */
  @Input() name = 'Upload';
  /** If multi-select files are enabled. Defaults to false (only single file) */
  @Input() multi = false; // Currently only single file upload handled
  /** The handler to process the uploaded thing */
  @Input() handler: (contents: any) => void;
  /** Optional handler that is called if the selected file could not be read / parsed as JSON. */
  @Input() errorHandler: (error: any) => void;

  /* Reference to this component's own file input (several upload buttons may be present on the same page). */
  @ViewChild('fileInput', { static: false }) fileUpload: ElementRef<HTMLInputElement>;

  constructor() {}

  onFileSelected() {
    const inputNode = this.fileUpload?.nativeElement;
    const file = inputNode?.files?.[0];
    if (!this.handler || !file) {
      return;
    }
    if (this.multi) {
      // TODO multi file upload
      console.error('UploadJsonButton: Cannot handle multi-select');
    }
    if (typeof FileReader !== 'undefined') {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        let parsed: any;
        try {
          parsed = JSON.parse(e.target.result);
        } catch (err) {
          console.error('UploadJsonButton: Selected file is not valid JSON', err);
          this.errorHandler?.(err);
          return;
        }
        this.handler(parsed);
      };
      reader.onerror = (err) => this.errorHandler?.(err);
      reader.readAsText(file);
    }
    /* Reset, so that selecting the same file again triggers another change event. */
    inputNode.value = '';
  }
}
