import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {TextQueryDescription} from '../../../../openapi';
import {map, withLatestFrom} from 'rxjs/operators';

@Component({
    selector: 'app-text-query-object-preview',
    templateUrl: './text-query-object-preview.component.html',
    styleUrls: ['./text-query-object-preview.component.scss']
})
export class TextQueryObjectPreviewComponent implements OnInit, OnDestroy {
    @Input() queryObject: Observable<TextQueryDescription>;
    @Input() timeElapsed: Observable<number>;

    currentText: Observable<string>;

    /** Font size in em. TODO: Make configurable. */
    fontSize = 2.5;

    ngOnInit(): void {
        this.currentText = this.timeElapsed.pipe(
            withLatestFrom(this.queryObject),
            map(([time, query]) => query.text.filter(t => t.showAfter < time).pop()?.text),
        );
    }

    ngOnDestroy(): void {

    }
}
