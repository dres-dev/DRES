import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subscription, timer } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { ApiCollectionScanStatus, CollectionService } from '../../../../openapi';

export interface CollectionScanDialogData {
  collectionId: string;
  collectionName: string;
  basePath: string;
}

/** Remembers the file endings last used for a scan in this browser. */
const STORAGE_KEY = 'dres.collection.scan.types';

/**
 * Dialog that starts a scan of a collection folder and shows its progress. The scan keeps running on the server
 * when the dialog is closed; reopening the dialog shows its current state again.
 */
@Component({
  selector: 'app-collection-scan-dialog',
  templateUrl: './collection-scan-dialog.component.html',
  styleUrls: ['./collection-maintenance.scss'],
  standalone: false,
})
export class CollectionScanDialogComponent implements OnInit, OnDestroy {
  videoTypes = 'mp4, webm, mkv, mov, avi, m4v';
  imageTypes = 'jpg, jpeg, png, webp, gif, bmp';

  status: ApiCollectionScanStatus = null;
  error: string = null;
  starting = false;

  /** True, if at least one item was added while this dialog was open (the item list should be refreshed). */
  changed = false;

  private polling: Subscription;

  constructor(
    private collectionService: CollectionService,
    public dialogRef: MatDialogRef<CollectionScanDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: CollectionScanDialogData
  ) {
    try {
      const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null');
      if (stored?.videoTypes !== undefined) this.videoTypes = stored.videoTypes;
      if (stored?.imageTypes !== undefined) this.imageTypes = stored.imageTypes;
    } catch (e) {
      /* Storage not available; keep defaults. */
    }
  }

  ngOnInit(): void {
    this.collectionService.getApiV2CollectionByCollectionIdScanStatus(this.data.collectionId).subscribe({
      next: (s) => {
        this.status = s;
        if (s.state === 'RUNNING') this.poll();
      },
      error: () => (this.status = null),
    });
  }

  ngOnDestroy(): void {
    this.polling?.unsubscribe();
  }

  get running(): boolean {
    return this.status?.state === 'RUNNING';
  }

  get progress(): number {
    if (!this.status?.filesFound) return 0;
    return (100 * this.status.filesProcessed) / this.status.filesFound;
  }

  start() {
    const split = (s: string) =>
      s
        .split(/[\s,;]+/)
        .map((t) => t.trim())
        .filter((t) => t.length > 0);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ videoTypes: this.videoTypes, imageTypes: this.imageTypes }));
    } catch (e) {
      /* Storage not available; ignore. */
    }
    this.error = null;
    this.starting = true;
    this.collectionService
      .postApiV2CollectionByCollectionIdScan(this.data.collectionId, {
        videoTypes: split(this.videoTypes),
        imageTypes: split(this.imageTypes),
      })
      .subscribe({
        next: (s) => {
          this.starting = false;
          this.status = s;
          this.poll();
        },
        error: (e) => {
          this.starting = false;
          this.error = e?.error?.description ?? e.message;
        },
      });
  }

  private poll() {
    this.polling?.unsubscribe();
    this.polling = timer(1000, 1000)
      .pipe(
        switchMap(() => this.collectionService.getApiV2CollectionByCollectionIdScanStatus(this.data.collectionId)),
        takeWhile((s) => s.state === 'RUNNING', true)
      )
      .subscribe({
        next: (s) => {
          if (s.itemsAdded > (this.status?.itemsAdded ?? 0)) this.changed = true;
          this.status = s;
          if (s.state !== 'RUNNING' && s.itemsAdded > 0) this.changed = true;
        },
        error: (e) => (this.error = e?.error?.description ?? e.message),
      });
  }

  close() {
    this.dialogRef.close(this.changed);
  }
}
