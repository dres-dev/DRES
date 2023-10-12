import { Pipe, PipeTransform } from '@angular/core';

/**
 * Transforms a given number as hours:minutes:seconds
 */
@Pipe({
  name: 'formatTime',
})
export class FormatTimePipePipe implements PipeTransform {
  transform(value: number, inMs = true): string {
    const divisor = inMs ? 1000 : 1;
    const hrs = Math.floor((value/divisor) / 3600);
    const mins = Math.floor(((value/divisor) % 3600) / 60);
    const secs = Math.floor((value/divisor) % 60);
    const ms = inMs ? Math.floor(value % divisor) : '';
    let out = '';
    /* Hours if present */
    if (hrs > 0) {
      out += '' + (''+hrs).padStart(2, '0') + ':';
    }
    /* Minutes */
    out += '' + (''+mins).padStart(2, '0') + ':';
    /* Seconds */
    out += '' + (''+secs).padStart(2, '0') + (inMs ? '.' : '');
    /* Milliseconds */
    if(inMs){
      out += '' + (''+ms).padStart(3,'0');
    }
    return out;
  }
}
