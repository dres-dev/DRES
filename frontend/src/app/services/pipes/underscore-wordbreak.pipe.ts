import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'underscoreWordBreak'
})
export class UnderscoreWordBreakPipe implements PipeTransform {

  /**
   * Simple pipe that adds the opportunity for the browser to break long words with underscores
   */
  transform(value: any): any{
    if(value){
      if(typeof value === 'string'){
        const doc = new DOMParser().parseFromString(value.replace(/_/g, "_&#8203;"), 'text/html');
        return doc.documentElement.textContent
      }else{
        return value
      }
    }
    return '';
  }

}
