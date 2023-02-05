import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { DataUtilities } from '../../utilities/data.utilities';
import {ApiHint} from '../../../../openapi';

@Component({
  selector: 'app-image-object-preview',
  template: `
    <div class="image-container" *ngIf="imageUrl | async" [style.text-align]="'center'">
      <img class="image" style="max-width: 100%;" [src]="imageUrl | async" alt="image" />
    </div>
  `,
})
export class ImageObjectPreviewComponent implements OnInit, OnDestroy {
  /** Observable of current {@link QueryContentElement} that should be displayed. */
  @Input() queryContent: Observable<ApiHint>;

  /** Current image to display (as data URL). */
  imageUrl: Observable<SafeUrl>;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.imageUrl = this.queryContent.pipe(
      filter((q) => q.type === 'IMAGE'),
      map((q) => {
        if (q.mediaItem) {
          // FIXME differ between external image and internal (i.e. media item). the following code is very likely broken
          return this.sanitizer.bypassSecurityTrustUrl(DataUtilities.base64ToUrl(q.mediaItem, 'image/jpg')); // FIXME should the content type be used from api q.dataType ?
        } else {
          return null;
        }
      })
    );
  }

  ngOnDestroy(): void {}
}
