import { AfterViewInit, Component, OnDestroy, ViewChild } from '@angular/core';
import { CollectionService, RestFullMediaCollection, RestMediaItem } from '../../../../openapi';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { BehaviorSubject, Observable, of, Subject, Subscription } from 'rxjs';
import { catchError, filter, flatMap, map, retry, shareReplay, switchMap } from 'rxjs/operators';
import { AppConfig } from '../../app.config';
import {
  MediaItemBuilderData,
  MediaItemBuilderDialogComponent,
} from '../collection-builder/media-item-builder-dialog/media-item-builder-dialog.component';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';

@Component({
  selector: 'app-collection-viewer',
  templateUrl: './collection-viewer.component.html',
  styleUrls: ['./collection-viewer.component.scss'],
})
export class CollectionViewerComponent implements AfterViewInit, OnDestroy {
  displayedColumns = ['actions', 'id', 'name', 'location', 'type', 'durationMs', 'fps'];

  /** Material Table UI element for sorting. */
  @ViewChild(MatSort) sort: MatSort;

  /** Material Table UI element for pagination. */
  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;

  /** Data source for Material tabl.e */
  dataSource = new MatTableDataSource<RestMediaItem>();

  /** Observable containing the collection ID of the collection displayed by this component. Derived from active route. */
  collectionId: Observable<string>;

  /** Observable containing the media collection information. */
  collection: Observable<RestFullMediaCollection>;

  /** A subject used to trigger refrehs of the list. */
  refreshSubject: Subject<null> = new BehaviorSubject(null);

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

    /*
     * Initialize subscription for collection data.
     *
     * IMPORTANT: Unsubscribe OnDestroy!
     */
    this.collection = this.refreshSubject.pipe(
      flatMap((s) => this.collectionId),
      switchMap((id) =>
        this.collectionService.getApiV1CollectionWithCollectionid(id).pipe(
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
    this.subscription = this.collection.subscribe((s: RestFullMediaCollection) => {
      this.dataSource.data = s.items;
    });
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
      this.collectionService.deleteApiV1MediaitemWithMediaid(id).subscribe(
        (r) => {
          this.refreshSubject.next();
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
        }
      );
    }
  }

  edit(id: string) {
    this.create(id);
  }

  show(id: string) {
    this.collectionId.subscribe((collectionId) => {
      window.open(this.mediaUrlForItem(collectionId, id), '_blank');
    });
  }

  create(id?: string) {
    this.collectionId.subscribe((colId: string) => {
      const config = { width: '500px' } as MatDialogConfig<Partial<MediaItemBuilderData>>;
      if (id) {
        config.data = { item: this.dataSource.data.find((it) => it.id === id), collectionId: colId } as MediaItemBuilderData;
      } else {
        config.data = { collectionId: colId } as Partial<MediaItemBuilderData>;
      }
      const dialogRef = this.dialog.open(MediaItemBuilderDialogComponent, config);
      dialogRef
        .afterClosed()
        .pipe(
          filter((r) => r != null),
          flatMap((r: RestMediaItem) => {
            if (id) {
              return this.collectionService.patchApiV1Mediaitem(r);
            } else {
              return this.collectionService.postApiV1Mediaitem(r);
            }
          })
        )
        .subscribe(
          (r) => {
            this.refreshSubject.next();
            this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
          },
          (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
          }
        );
    });
  }

  resolveMediaItemById(_: number, item: RestMediaItem) {
    return item.id;
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
