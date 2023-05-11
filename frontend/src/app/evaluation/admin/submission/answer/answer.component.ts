import { Component, Input } from "@angular/core";
import { ApiAnswer, ApiAnswerType, ApiTemporalPoint, ApiTemporalRange, ApiTemporalUnit } from "../../../../../../openapi";

@Component({
  selector: 'app-answer',
  templateUrl: './answer.component.html',
  styleUrls: ['./answer.component.scss']
})
export class AnswerComponent {

  @Input()
  public answers: ApiAnswer[];

  public displayedColumnsWithoutText = ['type', 'item', 'start', 'end', 'preview']
  public displayedColumnsText = ['type', 'text']
  public displayedHeaders = ['type', 'text', 'item', 'start', 'end', 'preview']

  /**
   *
   * @param answer
   */
  public transformToRange(answer: ApiAnswer): ApiTemporalRange | null{
    console.log(answer.temporalRange);
    if(answer.type == ApiAnswerType.TEMPORAL && answer.temporalRange){
      /* In case leakage of temporalRange is part of api */
      /*return {
        start: {value: answer.temporalRange.start['millisecond'], unit: ApiTemporalUnit.MILLISECONDS} as ApiTemporalPoint,
        end: {value: answer.temporalRange.start['millisecond'], unit: ApiTemporalUnit.MILLISECONDS} as ApiTemporalPoint,
      } as ApiTemporalRange;*/
      if(answer.start && answer.end){
        if(answer.start !== answer.end){
          return {
            start: {value: ''+answer.start, unit: ApiTemporalUnit.MILLISECONDS} as ApiTemporalPoint,
            end: {value: ''+answer.end, unit: ApiTemporalUnit.MILLISECONDS} as ApiTemporalPoint,
          } as ApiTemporalRange
        }
      }
    }
    return undefined;
  }

  public hasTextType = (index: number, rowData: ApiAnswer) => rowData.type === ApiAnswerType.TEXT;

  public hasNotTextType = (index: number, rowData: ApiAnswer) => rowData.type !== ApiAnswerType.TEXT;
}
