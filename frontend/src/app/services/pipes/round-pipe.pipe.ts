import { Pipe, PipeTransform } from '@angular/core';


/**
 * Rounds the given number to an integer.
 *
 * Usage:
 *  value | round
 *
 * Example 1:
 *  {{ 2.45432 | round }}
 *  formats to 2
 *
 * Example 2:
 *   {{ 4.6873 | round }}
 *   formats to 5
 */
@Pipe({
  name: 'round'
})
export class RoundPipePipe implements PipeTransform {

  transform(value: number): number {
    return Math.round(value);
  }

}
