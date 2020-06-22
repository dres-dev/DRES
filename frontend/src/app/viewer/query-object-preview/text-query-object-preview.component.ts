import {Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Observable, timer} from 'rxjs';
import {TextQueryDescription, TextualDescription} from '../../../../openapi';
import {concatMap, delayWhen, map, take, tap, withLatestFrom} from 'rxjs/operators';
import {AppConfig} from '../../app.config';
import {fromArray} from 'rxjs/internal/observable/fromArray';
import {AudioPlayerUtilities} from '../../utilities/audio-player.utilities';

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

    /** Reference to the audio element played when text changes. */
    @ViewChild('audio') audio: ElementRef<HTMLAudioElement>;

    constructor(public config: AppConfig) {}

    ngOnInit(): void {
        this.currentText = this.timeElapsed.pipe(
            take(1),
            withLatestFrom(this.queryObject),
            concatMap(([time, query]) => {
                return fromArray(query.text).pipe(
                    delayWhen<TextualDescription>(t => timer(1000 * Math.max(0, (t.showAfter - time)))),
                    map(t => t.text)
                );
            }),
            tap(t => {
                AudioPlayerUtilities.playOnce(this.audio.nativeElement);
            })
        );
    }

    ngOnDestroy(): void {}
}
