import { AfterViewInit, Component } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { CollectionBuilderDialogComponent } from '../collection-builder/collection-builder-dialog/collection-builder-dialog.component';
import { filter, mergeMap } from 'rxjs/operators';
import {ApiMediaCollection, CollectionService} from '../../../../openapi';

@Component({
  selector: 'app-collection-list',
  templateUrl: './collection-list.component.html',
  styleUrls: ['./collection-list.component.scss'],
})
export class CollectionListComponent implements AfterViewInit {
  displayedColumns = ['actions', 'id', 'name', 'description', 'basePath'];
  collections: ApiMediaCollection[] = [];

  constructor(
    private collectionService: CollectionService,
    private routerService: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  refresh() {
    this.collectionService.getApiV2CollectionList().subscribe({
      next: (results: ApiMediaCollection[]) => {
        this.collections = results;
      },
      error: (r) => {
        this.collections = [];
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    }
    );
  }

  ngAfterViewInit(): void {
    this.refresh();
  }

  create(id?: string) {
    const config = { width: '500px' } as MatDialogConfig<ApiMediaCollection>;
    if (id) {
      config.data = this.collections.find((c) => c.id === id);
    } else {
      config.data = null;
    }
    const dialogRef = this.dialog.open(CollectionBuilderDialogComponent, config);
    dialogRef
      .afterClosed()
      .pipe(
        filter((r) => r != null),
        mergeMap((r: ApiMediaCollection) => {
          if (id) {
            return this.collectionService.patchApiV2Collection(r);
          } else {
            return this.collectionService.postApiV2Collection(r);
          }
        })
      )
      .subscribe({
            next: (r) => {
              this.refresh();
              this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
            },
            error: (r) => {
              this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
            }
          }
      );
  }

  edit(id: string) {
    this.create(id);
  }

  delete(id: string) {
    if (confirm(`Do you really want to delete collection with ID ${id}?`)) {
      this.collectionService.deleteApiV2CollectionByCollectionId(id).subscribe({
        next: (r) => {
          this.refresh();
          this.snackBar.open(`Success: ${r.description}`, null, {duration: 5000});
        },
        error: (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, {duration: 5000});
        }
      });
    }
  }

  resolveMediaCollectionById(_: number, item: ApiMediaCollection) {
    return item.id;
  }
}
