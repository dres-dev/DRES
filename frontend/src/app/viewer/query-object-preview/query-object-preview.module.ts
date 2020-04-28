import {NgModule} from '@angular/core';
import {VideoQueryObjectPreviewComponent} from './video-query-object-preview.component';
import {TextQueryObjectPreviewComponent} from './text-query-object-preview.component';
import {CommonModule} from '@angular/common';

@NgModule({
    imports: [
        CommonModule
    ],
    exports:      [ VideoQueryObjectPreviewComponent, TextQueryObjectPreviewComponent ],
    declarations: [ VideoQueryObjectPreviewComponent, TextQueryObjectPreviewComponent ],
    providers:    [ ]
})
export class QueryObjectPreviewModule { }
