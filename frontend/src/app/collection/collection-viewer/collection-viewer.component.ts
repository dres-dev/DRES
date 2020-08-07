import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CollectionService, RestFullMediaCollection, RestMediaItem} from '../../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {Observable, of, Subscription} from 'rxjs';
import {catchError, filter, flatMap, map, retry, shareReplay, switchMap} from 'rxjs/operators';
import {AppConfig} from '../../app.config';
import {
    MediaItemBuilderData,
    MediaItemBuilderDialogComponent
} from '../collection-builder/media-item-builder-dialog/media-item-builder-dialog.component';
import {MatPaginator} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';

@Component({
    selector: 'app-collection-viewer',
    templateUrl: './collection-viewer.component.html',
    styleUrls: ['./collection-viewer.component.scss']
})
export class CollectionViewerComponent implements OnInit, OnDestroy {

    displayedColumns = ['actions', 'id', 'name', 'location', 'type', 'durationMs', 'fps'];

    @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
    dataSource = new MatTableDataSource<RestMediaItem>();

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
            this.dataSource.data = this.mediaItems;
            this.dataSource.paginator = this.paginator;
        });
    }

    delete(id: string) {
        if (confirm(`Do you really want to delete media item with ID ${id}?`)) {
            this.collectionService.deleteApiMediaitemWithMediaid(id).subscribe((r) => {
                this.refresh();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            });
        }
    }

    edit(id: string) {
        this.create(id);
    }

    ngOnDestroy(): void {
        this.itemsSub.unsubscribe();
    }

    show(id: string) {
        this.collectionId.subscribe((collectionId) => {
            window.open(this.mediaUrlForItem(collectionId, id), '_blank');
        });
    }

    create(id?: string) {
        this.collectionId.subscribe((colId: string) => {
            const config = {width: '500px'} as MatDialogConfig<Partial<MediaItemBuilderData>>;
            if (id) {
                config.data = {item: this.mediaItems.find(it => it.id === id), collectionId: colId} as MediaItemBuilderData;
            } else {
                config.data = {collectionId: colId} as Partial<MediaItemBuilderData>;
            }
            const dialogRef = this.dialog.open(MediaItemBuilderDialogComponent, config);
            dialogRef.afterClosed().pipe(
                filter(r => r != null),
                flatMap((r: RestMediaItem) => {
                    if (id) {
                        return this.collectionService.patchApiMediaitem(r);
                    } else {
                        return this.collectionService.postApiMediaitem(r);
                    }
                })
            ).subscribe((r) => {
                this.refresh();
                this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            }, (r) => {
                this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            });
        });
    }

    /**
     * Builds the routerLink array for the given id
     */
    private mediaUrlForItem(collectionId: string, id: string) {
        const url = this.config.resolveApiUrl(`media/${collectionId}/${id}`);
        console.log(url);
        return url;
    }
}
