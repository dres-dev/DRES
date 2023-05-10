import { Pipe, PipeTransform } from '@angular/core';
import { ApiMediaItem, ApiTemporalRange } from "../../../../openapi";
import { AppConfig } from "../../app.config";
import { TimeUtilities } from "../../utilities/time.utilities";

export interface MediaItemUrlOptions {
  range: ApiTemporalRange
}

function isOptions(obj: any): obj is MediaItemUrlOptions{
  return 'range' in obj;
}

@Pipe({
  name: 'resolveMediaItemUrl'
})
export class ResolveMediaItemUrlPipe implements PipeTransform {

  constructor(private config: AppConfig){

  }

  transform(value: ApiMediaItem, ...args: unknown[]): string {
    let suffix = '';
    if(args && args.length > 0){
      if(args[0] && isOptions(args[0])){
        const range = args[0].range;
        const startInSeconds = TimeUtilities.point2Milliseconds(range.start, value.fps) / 1000;
        const endInSeconds = TimeUtilities.point2Milliseconds(range.end, value.fps) / 1000;
        suffix = `#t=${startInSeconds},${endInSeconds}`;
      }
    }
    return this.config.resolveMediaItemUrl(`${value.mediaItemId}${suffix}`);
  }

}
