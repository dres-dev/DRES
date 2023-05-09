import { Component, Input } from "@angular/core";
import { ApiAnswer, ApiAnswerType } from "../../../../../../openapi";

@Component({
  selector: 'app-answer',
  templateUrl: './answer.component.html',
  styleUrls: ['./answer.component.scss']
})
export class AnswerComponent {

  @Input()
  public answers: ApiAnswer[];

  public displayedColumnsWithoutText = ['type', 'item', 'start', 'end']
  public displayedColumnsText = ['type', 'text']
  public displayedHeaders = ['type', 'text', 'item', 'start', 'end']

  public hasTextType = (index: number, rowData: ApiAnswer) => rowData.type === ApiAnswerType.TEXT;

  public hasNotTextType = (index: number, rowData: ApiAnswer) => rowData.type !== ApiAnswerType.TEXT;
}
