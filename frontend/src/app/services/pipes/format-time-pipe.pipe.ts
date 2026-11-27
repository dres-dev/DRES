import { Pipe, PipeTransform } from '@angular/core';

/**
 * Transforms a given number as hours:minutes:seconds.
 *
 * A perpetual task has no remaining time and reports `null`; rendering that as `00:00:00` claims a
 * deadline that does not exist, so `null` and `undefined` render as an infinity sign instead. Callers
 * that want a bare number can still pass `fallback`.
 *
 * @author DRES contributors
 */
@Pipe({
  name: 'formatTime',
  standalone: false,
})
export class FormatTimePipePipe implements PipeTransform {
  /**
   * @param value The duration to render; `null` or `undefined` means "no deadline".
   * @param inMs Whether `value` is milliseconds (`true`) or seconds (`false`).
   * @param fallback Text used for a missing duration.
   */
  transform(value: number | null | undefined, inMs = true, fallback = '\u221e'): string {
    /*
     * Guard first: the arithmetic below turns null into 0, and 0 is indistinguishable from an expired
     * countdown.
     */
    if (value == null || Number.isNaN(value)) {
      return fallback;
    }
    const divisor = inMs ? 1000 : 1;
    const hrs = Math.floor(value / divisor / 3600);
    const mins = Math.floor(((value / divisor) % 3600) / 60);
    const secs = Math.floor((value / divisor) % 60);
    const ms = inMs ? Math.floor(value % divisor) : '';
    let out = '';
    /* Hours if present */
    if (hrs > 0) {
      out += '' + ('' + hrs).padStart(2, '0') + ':';
    }
    /* Minutes */
    out += '' + ('' + mins).padStart(2, '0') + ':';
    /* Seconds */
    out += '' + ('' + secs).padStart(2, '0') + (inMs ? '.' : '');
    /* Milliseconds */
    if (inMs) {
      out += '' + ('' + ms).padStart(3, '0');
    }
    return out;
  }
}
