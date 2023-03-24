import { Pipe, PipeTransform } from '@angular/core';

/**
 * Transforms a given number as hours:minutes:seconds
 */
@Pipe({
  name: 'formatTime',
})
export class FormatTimePipePipe implements PipeTransform {
  transform(value: number): string {
    const hrs = Math.floor((value/1000) / 3600);
    const mins = Math.floor(((value/1000) % 3600) / 60);
    const secs = Math.floor((value/1000) % 60);
    const ms = Math.floor(value % 1000);
    let out = '';
    /* Hours if present */
    if (hrs > 0) {
      out += '' + (''+hrs).padStart(2, '0') + ':';
    }
    /* Minutes */
    out += '' + (''+mins).padStart(2, '0') + ':';
    /* Seconds */
    out += '' + (''+secs).padStart(2, '0') + '.';
    /* Milliseconds */
    out += '' + (''+ms).padStart(3,'0');
    return out;
  }
}
