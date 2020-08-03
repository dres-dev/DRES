import {Component, OnDestroy, OnInit} from '@angular/core';
import {CollectionService, RestFullMediaCollection, RestMediaItem} from '../../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog} from '@angular/material/dialog';
import {Observable, of, Subscription} from 'rxjs';
import {catchError, filter, map, retry, shareReplay, switchMap} from 'rxjs/operators';
import {AppConfig} from '../../app.config';

@Component({
    selector: 'app-collection-viewer',
    templateUrl: './collection-viewer.component.html',
    styleUrls: ['./collection-viewer.component.scss']
})
export class CollectionViewerComponent implements OnInit, OnDestroy {

    displayedColumns = ['actions', 'id', 'name', 'location', 'type', 'durationMs', 'fps'];

    collectionId: Observable<string>;
    collection: Observable<RestFullMediaCollection>;
    mediaItems: RestMediaItem[] = [];
    private itemsSub: Subscription;

    constructor(
        private collectionService: CollectionService,
        private activeRoute: ActivatedRoute,
        private snackBar: MatSnackBar,
        private router: Router,
        private dialog: MatDialog,
        private config: AppConfig
    ) {
    }

    ngOnInit(): void {
        this.refresh();
    }


    refresh() {
        /* The id observable from the route */
        this.collectionId = this.activeRoute.params.pipe(map(p => p.collectionId));

        /* The observable of the collection*/
        this.collection = this.collectionId.pipe(
            switchMap(id => this.collectionService.getApiCollectionWithCollectionid(id).pipe(
                retry(3),
                catchError((err, o) => {
                    console.log(`[CollectionViewer.${id}] There was an error while loading the current collection ${err?.message}`);
                    this.snackBar.open(`There was an error while loading the current collection ${err?.message}`);
                    return of(null);
                }),
                filter(q => q != null)
            )),
            shareReplay({bufferSize: 1, refCount: true})
        );
        /* Get the items from the observable */
        this.itemsSub = this.collection.subscribe((col) => {
            this.mediaItems = col.items.sort((a, b) => a.name.localeCompare(b.name));
        });
    }

    delete(id: string) {

    }

    edit(id: string) {

    }

    ngOnDestroy(): void {
        this.itemsSub.unsubscribe();
    }

    /**
     * Builds the routerLink array for the given id
     */
    private mediaUrlForItem(collectionId: string, id: string) {

        const url = this.config.resolveApiUrl(`media/${collectionId}/${id}`);
        console.log(url);
        return url;
    }

    show(id: string) {
        this.collectionId.subscribe((collectionid) => {
            window.open(this.mediaUrlForItem(collectionid, id), '_blank');
        });
    }
}
