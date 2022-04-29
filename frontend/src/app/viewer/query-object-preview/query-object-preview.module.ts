import { NgModule } from '@angular/core';
import { VideoObjectPreviewComponent } from './video-object-preview.component';
import { TextObjectPreviewComponent } from './text-object-preview.component';
import { CommonModule } from '@angular/common';
import { ImageObjectPreviewComponent } from './image-object-preview.component';

@NgModule({
  imports: [CommonModule],
  exports: [VideoObjectPreviewComponent, TextObjectPreviewComponent, ImageObjectPreviewComponent],
  declarations: [VideoObjectPreviewComponent, TextObjectPreviewComponent, ImageObjectPreviewComponent],
  providers: [],
})
export class QueryObjectPreviewModule {}
