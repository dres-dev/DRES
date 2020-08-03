import {Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Observable} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {AppConfig} from '../../app.config';
import {QueryContentElement} from '../../../../openapi';

@Component({
    selector: 'app-text-query-object-preview',
    template: `
        <div class="query-text-container">
            <p class="query-text" [style.font-size]="fontSize+'em'" [style.text-align]="'center'" [style.line-height]="'1.5em'">{{currentText | async}}</p>
        </div>
    `
})
export class TextQueryObjectPreviewComponent implements OnInit, OnDestroy {

    /** Observable of current {@link QueryContentElement} that should be displayed. */
    @Input() queryContent: Observable<QueryContentElement>;

    /** Current text to display. */
    currentText: Observable<string>;

    /** Font size in em. TODO: Make configurable. */
    fontSize = 2.5;

    /** Reference to the audio element played when text changes. */
    @ViewChild('audio') audio: ElementRef<HTMLAudioElement>;

    constructor(public config: AppConfig) {}

    ngOnInit(): void {
        this.currentText = this.queryContent.pipe(
            filter(q => q.contentType === 'TEXT'),
            map(q => q.content)
        );
    }

    ngOnDestroy(): void {}
}
