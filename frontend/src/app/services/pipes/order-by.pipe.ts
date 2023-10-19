import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: "orderBy"
})
export class OrderByPipe implements PipeTransform {

  transform(value: any[], order = "", compare: (a: any, b: any) => number): any[] {
    if (!value || order === "" || !order) {
      return value;
    }
    if (value.length <= 1) {
      return value;
    }
    if (!compare) {
      if (order === "asc") {
        return value.sort();
      } else {
        return value.sort().reverse();
      }
    } else {
      return value.sort(compare);
    }
  }

}
