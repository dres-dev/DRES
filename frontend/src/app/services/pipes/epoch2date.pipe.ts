import { Pipe, PipeTransform } from '@angular/core';

/**
 * Converts unix timestamp to Date type
 */
@Pipe({
  name: 'epoch2date',
})
export class Epoch2DatePipePipe implements PipeTransform {
  transform(value: number): Date {
    return new Date(value);
  }
}
