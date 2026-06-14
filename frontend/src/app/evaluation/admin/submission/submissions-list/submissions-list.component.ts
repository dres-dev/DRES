import { AfterViewInit, Component, OnDestroy, ViewChild } from '@angular/core';
import { merge, Observable, of, Subject, Subscription, timer } from 'rxjs';
import { MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import {
  ApiSubmission,
  ApiSubmissionInfo,
  ApiTaskTemplate,
  EvaluationAdministratorService,
  EvaluationService,
<<<<<<< HEAD
  TemplateService,
} from '../../../../../../openapi';
import { AppConfig } from '../../../../app.config';
import { catchError, filter, map, switchMap, take, withLatestFrom } from 'rxjs/operators';
import { WebSocketService } from '../../../../services/websocket.service';
import { ServerMessageType } from '../../../../model/ws/server-message-type.enum';
=======
  TemplateService
} from "../../../../../../openapi";
import { AppConfig } from "../../../../app.config";
import { catchError, filter, map, switchMap, take, withLatestFrom } from "rxjs/operators";
import { WebSocketService } from "../../../../services/websocket.service";
import { ServerMessageType } from "../../../../model/ws/server-message-type.enum";
>>>>>>> d4e51a229d945ba8e81455762fa7bf0759518b7a

@Component({
  selector: 'app-submissions-list',
  templateUrl: './submissions-list.component.html',
  styleUrls: ['./submissions-list.component.scss'],
  standalone: false,
})
export class SubmissionsListComponent implements AfterViewInit, OnDestroy {
  @ViewChild('toggleGroup', { static: true }) toggleGroup: MatButtonToggleGroup;

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

<<<<<<< HEAD
  constructor(
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private activeRoute: ActivatedRoute,
    private evalService: EvaluationService,
    private evaluationService: EvaluationAdministratorService,
    private templateService: TemplateService,
    public config: AppConfig,
    private wsService: WebSocketService
  ) {
    this.runId = this.activeRoute.paramMap.pipe(map((params) => params.get('runId')));
    this.taskId = this.activeRoute.paramMap.pipe(map((params) => params.get('taskId')));
  }
  ngAfterViewInit(): void {
    this.runId.pipe(take(1)).subscribe((id) => this.wsService.connect(id));

    /* Refresh on new/updated submissions pushed via WebSocket, in addition to manual refresh and periodic polling. */
    const wsRefresh$ = this.wsService.messages$.pipe(
      filter((msg) =>
        [ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED, ServerMessageType.ServerMessageTypeEnum.TASK_END].includes(msg.type)
      )
    );

    this.subscription = merge(
      timer(0, this.pollingFrequencyInSeconds * 1000).pipe(filter((_) => this.polling)),
      this.refreshSubject,
      wsRefresh$
    )
      .pipe(
        withLatestFrom(this.runId, this.taskId),
        switchMap(([_, r, t]) => this.evaluationService.getApiV2EvaluationAdminByEvaluationIdSubmissionListByTemplateId(r, t)),
        catchError((err, o) => {
          console.error(`[SubmissionList] Error occurred while laoding submissions: ${err?.message}`);
          this.snackBar.open(`Error: Couldn't load submissions for reason: ${err?.message}`, null, { duration: 5000 });
          return of([]);
        })
      )
      .subscribe((s: ApiSubmissionInfo[]) => {
        /* The assumption here is, that task runs do not magically disappear */
        if (this.taskRunIds.length < s.length) {
          s.forEach((si) => {
            if (!this.taskRunIds.includes(si.taskId)) {
              this.taskRunIds.push(si.taskId);
              this.submissionInfosByRunId.set(si.taskId, si);
            }
          });
        }
      });
    this.sub = this.runId
      .pipe(
        switchMap((r) => this.evalService.getApiV2EvaluationByEvaluationIdInfo(r)),
        catchError((error, o) => {
          console.log(`[SubmissionList] Error occurred while loading template information: ${error?.message}`);
          this.snackBar.open(`Error: Couldn't load template information: ${error?.message}`, null, { duration: 5000 });
          return of(null);
        }),
        filter((r) => r != null),
        switchMap((evalInfo) => this.templateService.getApiV2TemplateByTemplateIdTaskList(evalInfo.templateId)),
        withLatestFrom(this.taskId)
      )
      .subscribe(([taskList, taskId]) => {
        this.taskTemplate = taskList.find((t) => t.id === taskId);
      });
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
    this.subscription?.unsubscribe();
    this.subscription = null;
    this.sub?.unsubscribe();
    this.sub = null;
=======
   private sub: Subscription;


   constructor(
     private snackBar: MatSnackBar,
     private dialog: MatDialog,
     private activeRoute: ActivatedRoute,
     private evalService: EvaluationService,
     private evaluationService: EvaluationAdministratorService,
     private templateService: TemplateService,
     public config: AppConfig,
     private wsService: WebSocketService,
   ) {
     this.runId = this.activeRoute.paramMap.pipe(map((params) => params.get('runId')));
     this.taskId = this.activeRoute.paramMap.pipe(map((params) => params.get('taskId')));
   }
  ngAfterViewInit(): void {
     this.runId.pipe(take(1)).subscribe((id) => this.wsService.connect(id));

     /* Refresh on new/updated submissions pushed via WebSocket, in addition to manual refresh and periodic polling. */
     const wsRefresh$ = this.wsService.messages$.pipe(
       filter((msg) => [
         ServerMessageType.ServerMessageTypeEnum.TASK_UPDATED,
         ServerMessageType.ServerMessageTypeEnum.TASK_END,
       ].includes(msg.type))
     );

     this.subscription = merge(
       timer(0, this.pollingFrequencyInSeconds * 1000)
         .pipe(filter((_) => this.polling)),
       this.refreshSubject,
       wsRefresh$)
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
     this.wsService.disconnect();
     this.subscription?.unsubscribe();
     this.subscription = null;
     this.sub?.unsubscribe();
     this.sub = null;
>>>>>>> d4e51a229d945ba8e81455762fa7bf0759518b7a
  }

  trackById(_: number, item: ApiSubmissionInfo) {
    return item.taskId;
  }

  trackBySelf(_: number, item: string) {
    return item;
  }
}
