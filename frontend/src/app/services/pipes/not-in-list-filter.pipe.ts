import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'notInListFilter'
})
export class NotInListFilterPipe implements PipeTransform {

  transform(list: any[], filter: any): any[] {
    if(!list){
      return [];
    }
    if(!filter){
      return list;
    }
    return list.filter(it => it != filter);
  }

}
