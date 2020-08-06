import {Component, Input, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {AppConfig} from '../../app.config';
import {ContentElement} from '../../../../openapi';

@Component({
    selector: 'app-text-object-preview',
    template: `
        <div class="text-container" *ngIf="(currentText | async)">
            <p class="text" [style.font-size]="fontSize+'em'" [style.text-align]="'center'" [style.line-height]="'1.5em'">{{currentText | async}}</p>
        </div>
    `
})
export class TextObjectPreviewComponent implements OnInit {
    /** Observable of current {@link QueryContentElement} that should be displayed. */
    @Input() queryContent: Observable<ContentElement>;

    /** Current text to display. */
    currentText: Observable<string>;

    /** Font size in em. TODO: Make configurable. */
    fontSize = 2.5;

    constructor(public config: AppConfig) {}

    ngOnInit(): void {
        this.currentText = this.queryContent.pipe(
            filter(q => q.contentType === 'TEXT'),
            map(q => {
                if (q.content) {
                    return q.content;
                } else {
                    return null;
                }
            })
        );
    }
}
