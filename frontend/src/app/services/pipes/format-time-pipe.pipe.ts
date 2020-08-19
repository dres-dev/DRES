import { Pipe, PipeTransform } from '@angular/core';


/**
 * Transforms a given number as hours:minutes:seconds
 */
@Pipe({
  name: 'formatTime'
})
export class FormatTimePipePipe implements PipeTransform {

  transform(value: number): string {
    const hrs = Math.floor(value / 3600);
    const mins = Math.floor((value % 3600) / 60);
    const secs = Math.floor(value % 60);
    let out = '';
    /* Hours if present */
    if(hrs > 0){
      out += '' + (hrs < 10 ? '0' : '') + hrs + ':';
    }
    /* Minutes */
    out += '' + (mins < 10 ? '0' : '') + mins + ':';
    /* seconds */
    out += '' + (secs < 10 ? '0' : '') + secs;
    return out;
  }

}
