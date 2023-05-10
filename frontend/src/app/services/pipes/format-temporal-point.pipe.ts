import { Pipe, PipeTransform } from '@angular/core';
import { ApiTemporalPoint } from "../../../../openapi";
import { FormatTemporalUnitPipe } from "./format-temporal-unit.pipe";
import { FormatTimePipePipe } from "./format-time-pipe.pipe";

@Pipe({
  name: 'formatTemporalPoint'
})
export class FormatTemporalPointPipe implements PipeTransform {

  constructor(
    private unitPipe: FormatTemporalUnitPipe,
    private timePipe: FormatTimePipePipe,
  ) {}
  transform(value: ApiTemporalPoint, ...args: unknown[]): string {
    switch(value.unit){
      case "FRAME_NUMBER":
      case "SECONDS":
        return `${value.value}${this.unitPipe.transform(value.unit)}`;
      case "MILLISECONDS":
        return `${Number(value.value) / 1000}${this.unitPipe.transform(value.unit)}`;
      case "TIMECODE":
        return this.timePipe.transform(Number(value.value));

    }
  }

}
