import { AfterViewInit, Component, OnDestroy, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import {BehaviorSubject, mergeMap, Observable, of, Subject, Subscription} from 'rxjs';
import { catchError, filter, map, retry, shareReplay, switchMap } from 'rxjs/operators';
import { AppConfig } from '../../app.config';
import {
  MediaItemBuilderData,
  MediaItemBuilderDialogComponent,
} from '../collection-builder/media-item-builder-dialog/media-item-builder-dialog.component';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import {ApiMediaItem, ApiPopulatedMediaCollection, CollectionService} from '../../../../openapi';

@Component({
  selector: 'app-collection-viewer',
  templateUrl: './collection-viewer.component.html',
  styleUrls: ['./collection-viewer.component.scss'],
})
export class CollectionViewerComponent implements AfterViewInit, OnDestroy {

  public isLoading = true;

  displayedColumns = ['actions', 'id', 'name', 'location', 'type', 'durationMs', 'fps'];

  /** Material Table UI element for sorting. */
  @ViewChild(MatSort) sort: MatSort;

  /** Material Table UI element for pagination. */
  @ViewChild('paginator') paginator: MatPaginator;

  /** Data source for Material tabl.e */
  dataSource = new MatTableDataSource<ApiMediaItem>();

  /** Observable containing the collection ID of the collection displayed by this component. Derived from active route. */
  collectionId: Observable<string>;

  /** Observable containing the media collection information. */
  collection: Observable<ApiPopulatedMediaCollection>;

  /** A subject used to trigger refrehs of the list. */
  refreshSubject: Subject<void> = new BehaviorSubject(null);

  /** Reference to the subscription held by this component. */
  private subscription: Subscription;

  constructor(
    private collectionService: CollectionService,
    private activeRoute: ActivatedRoute,
    private snackBar: MatSnackBar,
    private router: Router,
    private dialog: MatDialog,
    private config: AppConfig
  ) {
    this.collectionId = this.activeRoute.params.pipe(map((p) => p.collectionId));
  }

  /**
   * Register subscription for submission data;
   *
   * TODO: In this implementation, pagination is done on the client side!
   */
  ngAfterViewInit(): void {
    /* Initialize sorting and pagination. */
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    /* Custom filter: on ID, Name and Location */
    this.dataSource.filterPredicate = (data: ApiMediaItem, value: string) => data.mediaItemId.includes(value) || data.name.includes(value) || data.location.includes(value)

    /*
     * Initialize subscription for collection data.
     *
     * IMPORTANT: Unsubscribe OnDestroy!
     */
    this.collection = this.refreshSubject.pipe(
      mergeMap((s) => this.collectionId),
      switchMap((id) =>
        this.collectionService.getApiV2CollectionByCollectionId(id).pipe(
          retry(3),
          catchError((err, o) => {
            console.log(`[CollectionViewer.${id}] There was an error while loading the current collection ${err?.message}`);
            this.snackBar.open(`There was an error while loading the current collection ${err?.message}`, null, {
              duration: 5000,
            });
            return of(null);
          }),
          filter((q) => q != null)
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.subscription = this.collection.subscribe((s: ApiPopulatedMediaCollection) => {
      this.dataSource.data = s.items;
      this.isLoading = false;
    });
  }

  applyFilter(event: Event){
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if(this.dataSource.paginator){
      this.dataSource.paginator.firstPage();
    }
  }

  /**
   * House keeping; clean up subscriptions.
   */
  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.subscription = null;
  }

  delete(id: string) {
    if (confirm(`Do you really want to delete media item with ID ${id}?`)) {
      this.collectionService.deleteApiV2MediaItemByMediaId(id).subscribe({
        next: (r) => {
          this.refreshSubject.next();
          this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
        },
        error: (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
        }
      });
    }
  }

  edit(id: string) {
    this.create(id);
  }

  show(id: string) {
    this.collectionId.subscribe((collectionId) => {
      window.open(this.mediaUrlForItem(id), '_blank');
    });
  }

  create(id?: string) {
    this.collectionId.subscribe((colId: string) => {
      const config = { width: '500px' } as MatDialogConfig<Partial<MediaItemBuilderData>>;
      if (id) {
        config.data = { item: this.dataSource.data.find((it) => it.mediaItemId === id), collectionId: colId } as MediaItemBuilderData;
      } else {
        config.data = { collectionId: colId } as Partial<MediaItemBuilderData>;
      }
      const dialogRef = this.dialog.open(MediaItemBuilderDialogComponent, config);
      dialogRef
        .afterClosed()
        .pipe(
          filter((r) => r != null),
          mergeMap((r: ApiMediaItem) => {
            if (id) {
              return this.collectionService.patchApiV2Mediaitem(r);
            } else {
              return this.collectionService.postApiV2MediaItem(r);
            }
          })
        )
        .subscribe({
          next: (r) => {
            this.refreshSubject.next();
            this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
          },
          error: (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
          }
        });
    });
  }

  resolveMediaItemById(_: number, item: ApiMediaItem) {
    return item.mediaItemId;
  }

  /**
   * Builds the routerLink array for the given id
   */
  private mediaUrlForItem(id: string) {
    const url = this.config.resolveApiUrl(`media/${id}`);
    console.log(url);
    return url;
  }
}
