import { Pipe, PipeTransform } from '@angular/core';
import { ApiMediaItem, CollectionService } from "../../../../openapi";
import { Observable } from "rxjs";

@Pipe({
  name: 'resolveMediaItem'
})
export class ResolveMediaItemPipe implements PipeTransform {

  private cachedItem: Observable<ApiMediaItem> | null = null
  private cachedId: string = ''

  constructor(
    private mediaService: CollectionService,
  ){}

  transform(value: string, ...args: unknown[]): Observable<ApiMediaItem> {
    if(value !== this.cachedId){
      this.cachedItem = null;
      this.cachedId = value;
      this.cachedItem = this.mediaService.getApiV2MediaItemByMediaItemId(value);
    }
    console.log(this.cachedItem)
    return this.cachedItem;
  }

}
