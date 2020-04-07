import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Component({
    selector: 'app-run-viewer-list',
    templateUrl: './run-viewer.component.html'
})
export class RunViewerComponent  {


    runId: Observable<number>;

    constructor(activeRoute: ActivatedRoute) {
        this.runId = activeRoute.params.pipe(
            map(p => p.runId)
        );
    }
}
