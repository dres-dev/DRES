import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'spaceToNewline'
})
export class SpaceToNewlinePipe implements PipeTransform {

  transform(value: string, lineBreak= false): string {
    const nl: string = lineBreak ? '<br />' : '\n';
    return value.split(' ').join(nl);
  }

}
