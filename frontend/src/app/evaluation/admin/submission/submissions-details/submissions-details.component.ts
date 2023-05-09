import { Component, ComponentFactoryResolver, Input } from "@angular/core";
import { ApiSubmission, ApiSubmissionInfo } from "../../../../../../openapi";
import { animate, state, style, transition, trigger } from "@angular/animations";

@Component({
  selector: 'app-submissions-details',
  templateUrl: './submissions-details.component.html',
  styleUrls: ['./submissions-details.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4,0.0,0.2,1)')),
    ]),
  ],
})
export class SubmissionsDetailsComponent {

  @Input()
  public submission: ApiSubmissionInfo;

  public columnsToDisplay = ['submission-id', 'timestamp', 'author', 'nbAnswers'];
  public columnsToDisplayWithExpand = [...this.columnsToDisplay, 'expand']
  public expandedElement: ApiSubmission | null;

  trackById(_:number, item: ApiSubmission){
    return item.submissionId;
  }

  update(element: any, value: any){
    console.log("UPDATE", element, value);
  }
}
