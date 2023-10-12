import { Pipe, PipeTransform } from '@angular/core';
import { AppConfig } from "../../app.config";
import { ApiMediaItem } from "../../../../openapi";
import { TimeUtilities } from "../../utilities/time.utilities";
import { isMediaItemUrlOptions } from "./resolve-media-item-url.pipe";

@Pipe({
  name: 'resolveMediaItemPreview'
})
export class ResolveMediaItemPreviewPipe implements PipeTransform {

  constructor(private config: AppConfig){

  }

  transform(value: ApiMediaItem, ...args: unknown[]): string {
    let suffix = '';
    if(args && args.length > 0){
      if(args[0] && isMediaItemUrlOptions(args[0]) && args[0].time){
        return this.config.resolveImagePreviewUrl(value.mediaItemId, `${args[0].time}`)
      }
    }
    return this.config.resolveImagePreviewUrl(value.mediaItemId)
  }

}
