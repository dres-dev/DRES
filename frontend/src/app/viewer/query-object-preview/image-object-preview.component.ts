import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { DataUtilities } from '../../utilities/data.utilities';
import { AppConfig } from '../../app.config';
import { ApiContentElement } from '../../../../openapi';

@Component({
  selector: 'app-image-object-preview',
  template: `
    <div class="image-container" *ngIf="imageUrl | async" [style.text-align]="'center'">
      <img class="image" style="max-width: 100%;" [src]="imageUrl | async" alt="image" />
    </div>
  `,
  standalone: false,
})
export class ImageObjectPreviewComponent implements OnInit {
  /** Observable of current {@link QueryContentElement} that should be displayed. */
  @Input() queryContent: Observable<ApiContentElement>;

  /** Current image to display (as data URL or grant URL). */
  imageUrl: Observable<SafeUrl>;

  constructor(
    private sanitizer: DomSanitizer,
    private config: AppConfig
  ) {}

  ngOnInit(): void {
    this.imageUrl = this.queryContent.pipe(
      filter((q) => q.contentType === 'IMAGE'),
      map((q) => {
        /* See the video preview: prefer the session-bound grant URL over an inline base64 blob. */
        if (q.mediaUrl) {
          return this.sanitizer.bypassSecurityTrustUrl(this.config.resolveMediaGrantUrl(q.mediaUrl));
        }
        if (q.content) {
          return this.sanitizer.bypassSecurityTrustUrl(DataUtilities.base64ToUrl(q.content, 'image/jpg'));
        }
        return null;
      })
    );
  }
}
