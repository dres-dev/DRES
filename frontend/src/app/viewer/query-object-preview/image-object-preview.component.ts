import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {QueryContentElement} from '../../../../openapi';
import {filter, map} from 'rxjs/operators';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {DataUtilities} from '../../utilities/data.utilities';

@Component({
    selector: 'app-image-object-preview',
    template: `
        <div class="image-container" *ngIf="(imageUrl | async)">
            <img class="image" [src]="(imageUrl | async)" alt="image">
        </div>
    `
})
export class ImageObjectPreviewComponent implements OnInit, OnDestroy {
    /** Observable of current {@link QueryContentElement} that should be displayed. */
    @Input() queryContent: Observable<QueryContentElement>;

    /** Current image to display (as data URL). */
    imageUrl: Observable<SafeUrl>;

    constructor(private sanitizer: DomSanitizer) {}

    ngOnInit(): void {
        this.imageUrl = this.queryContent.pipe(
            filter(q => q.contentType === 'IMAGE'),
            map(q => {
                if (q.content) {
                    return this.sanitizer.bypassSecurityTrustUrl(DataUtilities.base64ToUrl(q.content, 'image/jpg'));
                } else {
                    return null;
                }
            })
        );
    }

    ngOnDestroy(): void {}
}
