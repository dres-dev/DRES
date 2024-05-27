import { Pipe, PipeTransform } from '@angular/core';

/**
 * Simple pipe to filter an array such that those element not in the haytack are kept.
 */
@Pipe({
  name: 'filterNotIn'
})
export class FilterNotInPipe implements PipeTransform {

  /**
   * Filters a given array such that none of the elements in haystack are in the resulting array.
   *
   * @param value The array to filter
   * @param haystack The haytack to search in
   * @param propertyKey The key of the property to compare on.
   */
  transform(value: any[], haystack: any[], propertyKey : string = null): any[] {
    if(value){
      if(propertyKey){
       return value.filter(it => !haystack.map(item => item != null && item[propertyKey]).includes(it[propertyKey]))
      }
      return value.filter(it => !haystack.includes(it));
    }else{
      return [];
    }
  }

}
