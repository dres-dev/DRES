import { Pipe, PipeTransform } from '@angular/core';
import { ApiTemporalUnit } from "../../../../openapi";

@Pipe({
  name: 'formatTemporalUnit'
})
export class FormatTemporalUnitPipe implements PipeTransform {

  transform(value: ApiTemporalUnit, ...args: unknown[]): unknown {
    switch(value){
      case "FRAME_NUMBER":
        return "f";
      case "SECONDS":
        return "s";
      case "MILLISECONDS":
        return "ms";
      case "TIMECODE":
        return "";
    }
  }

}
