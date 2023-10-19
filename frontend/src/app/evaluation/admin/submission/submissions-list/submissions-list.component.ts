import { AfterViewInit, Component, OnDestroy, ViewChild } from "@angular/core";
import { merge, Observable, of, Subject, Subscription, timer } from "rxjs";
import { MatButtonToggleGroup } from "@angular/material/button-toggle";
import { MatSnackBar } from "@angular/material/snack-bar";
import { MatDialog } from "@angular/material/dialog";
import { ActivatedRoute } from "@angular/router";
import {
  ApiSubmission,
  ApiSubmissionInfo, ApiTaskTemplate,
  EvaluationAdministratorService,
  EvaluationService,
  TemplateService
} from "../../../../../../openapi";
import { AppConfig } from "../../../../app.config";
import { catchError, filter, map, switchMap, withLatestFrom } from "rxjs/operators";

@Component({
  selector: 'app-submissions-list',
  templateUrl: './submissions-list.component.html',
  styleUrls: ['./submissions-list.component.scss']
})
export class SubmissionsListComponent implements AfterViewInit, OnDestroy{

  @ViewChild('toggleGroup', {static: true}) toggleGroup: MatButtonToggleGroup;

   public runId: Observable<string>;
   public taskId: Observable<string>;

   public pollingFrequencyInSeconds = 30;

   public polling = true;

   public anonymize = true;

   public refreshSubject: Subject<null> = new Subject();

   public taskRunIds: string[] = [];
   public submissionInfosByRunId: Map<string, ApiSubmissionInfo> = new Map();

   public taskTemplate: ApiTaskTemplate;

   private subscription: Subscription;

   private sub: Subscription;


   constructor(
     private snackBar: MatSnackBar,
     private dialog: MatDialog,
     private activeRoute: ActivatedRoute,
     private evalService: EvaluationService,
     private evaluationService: EvaluationAdministratorService,
     private templateService: TemplateService,
     public config: AppConfig,
   ) {
     this.runId = this.activeRoute.paramMap.pipe(map((params) => params.get('runId')));
     this.taskId = this.activeRoute.paramMap.pipe(map((params) => params.get('taskId')));
   }
  ngAfterViewInit(): void {
     this.subscription = merge(
       timer(0, this.pollingFrequencyInSeconds * 1000)
         .pipe(filter((_) => this.polling)),
       this.refreshSubject)
       .pipe(
         withLatestFrom(this.runId, this.taskId),
         switchMap(([_,r,t]) => this.evaluationService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId(r,t)),
         catchError((err, o) => {
           console.error(`[SubmissionList] Error occurred while laoding submissions: ${err?.message}`);
           this.snackBar.open(`Error: Couldn't load submissions for reason: ${err?.message}`, null, {duration: 5000});
           return of([]);
         })
       )
       .subscribe((s: ApiSubmissionInfo[]) => {
         /* The assumption here is, that task runs do not magically disappear */
         if(this.taskRunIds.length < s.length){
           s.forEach((si) => {
             if(!this.taskRunIds.includes(si.taskId)){
               this.taskRunIds.push(si.taskId);
               this.submissionInfosByRunId.set(si.taskId, si);
             }
           })
         }
       })
    this.sub = this.runId.pipe(
      switchMap((r) => this.evalService.getApiV2EvaluationByEvaluationIdInfo(r)),
      catchError((error, o) => {
        console.log(`[SubmissionList] Error occurred while loading template information: ${error?.message}`);
        this.snackBar.open(`Error: Couldn't load template information: ${error?.message}`, null, {duration: 5000});
        return of(null);
      }),
      filter((r) => r != null),
      switchMap((evalInfo) => this.templateService.getApiV2TemplateByTemplateIdTaskList(evalInfo.templateId)),
      withLatestFrom(this.taskId)
    ).subscribe(([taskList, taskId]) => {
      this.taskTemplate = taskList.find((t) => t.id === taskId)
    });
  }

  ngOnDestroy(): void {
     this.subscription?.unsubscribe();
     this.subscription = null;
     this.sub?.unsubscribe();
     this.sub = null;
  }

  trackById(_: number, item: ApiSubmissionInfo){
     return item.taskId;
  }

  trackBySelf(_: number, item: string){
     return item;
  }

}
