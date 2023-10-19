import { Pipe, PipeTransform } from '@angular/core';
import { ApiMediaItem } from "../../../../openapi";

export interface MediaItemDisplayOptions {
  showId?: boolean
  shortenId?: boolean
  showType?: boolean
}

@Pipe({
  name: 'formatMediaItem'
})
export class FormatMediaItemPipe implements PipeTransform {

  transform(value: ApiMediaItem, options?: MediaItemDisplayOptions): string {
    if(value){
      let out = value.name
      if(options){
        if(options.showId){
          const end = options.shortenId ? 8 : value.mediaItemId.length
          out += ' ('+value.mediaItemId.substring(0,end)+')'
        }
        if(options.showType){
          out += ' (' + value.type + ')'
        }
      }
      return out;
    }
    return ''
  }

}
