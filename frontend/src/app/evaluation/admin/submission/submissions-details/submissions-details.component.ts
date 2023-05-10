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

  public expandedElements: ApiSubmission[] = [];

  public isExpanded(element: ApiSubmission){
    return this.expandedElements.includes(element);
  }

  public toggleExpanded(element: ApiSubmission){
    if(this.isExpanded(element)){
      this.expandedElements.splice(this.expandedElements.indexOf(element), 1);
    }else{
      this.expandedElements.push(element);
    }
  }

  trackById(_:number, item: ApiSubmission){
    return item.submissionId;
  }

  update(element: any, value: any){
    console.log("UPDATE", element, value);
  }
}
