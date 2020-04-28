import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {TextQueryDescription} from '../../../../openapi';

@Component({
    selector: 'app-text-query-object-preview',
    templateUrl: './text-query-object-preview.component.html',
    styleUrls: ['./text-query-object-preview.component.scss']
})
export class TextQueryObjectPreviewComponent implements OnInit, OnDestroy {
    @Input() queryObject: Observable<TextQueryDescription>;
    @Input() timeElapsed: Observable<number>;

    /** Font size in em. TODO: Make configurable. */
    fontSize = 2.0;


    ngOnDestroy(): void {
    }

    ngOnInit(): void {
    }
}
