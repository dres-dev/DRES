import { NgModule } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { CollectionViewerComponent } from './collection-viewer/collection-viewer.component';
import { CollectionListComponent } from './collection-list/collection-list.component';
import { MediaItemBuilderDialogComponent } from './collection-builder/media-item-builder-dialog/media-item-builder-dialog.component';
import { CollectionBuilderDialogComponent } from './collection-builder/collection-builder-dialog/collection-builder-dialog.component';
import { MatOptionModule } from '@angular/material/core';
import { SharedModule } from '../shared/shared.module';
import { RouterModule } from '@angular/router';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { MatSortModule } from '@angular/material/sort';
import { MatSelectModule } from '@angular/material/select';

@NgModule({
  imports: [
    FormsModule,
    ReactiveFormsModule,
    CommonModule,
    SharedModule,
    RouterModule,
    ClipboardModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatSelectModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatOptionModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatMenuModule,
  ],
  exports: [CollectionViewerComponent, CollectionListComponent],
  declarations: [
    CollectionViewerComponent,
    CollectionListComponent,
    MediaItemBuilderDialogComponent,
    CollectionBuilderDialogComponent,
  ],
  providers: [],
})
export class CollectionModule {}
