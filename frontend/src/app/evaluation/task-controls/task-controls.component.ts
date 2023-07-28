import { Component, Input, OnInit } from "@angular/core";
import { Observable, Subject } from "rxjs";
import { CombinedRun } from "../../run/run-admin-view.component";
import { map, switchMap } from "rxjs/operators";
import { ActivatedRoute, Router } from "@angular/router";
import { ApiEvaluationState, EvaluationAdministratorService, EvaluationService, TemplateService } from "../../../../openapi";
import { MatSnackBar } from "@angular/material/snack-bar";
import { MatDialog } from "@angular/material/dialog";
import {
  ConfirmationDialogComponent,
  ConfirmationDialogComponentData
} from "../../shared/confirmation-dialog/confirmation-dialog.component";
import { AuthenticationService } from "../../services/session/authentication.sevice";

@Component({
  selector: 'app-task-controls',
  templateUrl: './task-controls.component.html',
  styleUrls: ['./task-controls.component.scss']
})
export class TaskControlsComponent implements OnInit{

  @Input() runState: Observable<ApiEvaluationState>;
  @Input() refreshSubject: Subject<void> = new Subject();

  @Input() showTime: boolean = false;

  runId: Observable<string>;

  constructor(
    private runAdminService: EvaluationAdministratorService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    public authenticationService: AuthenticationService
  ) {
  }

  ngOnInit(): void {
        this.runId = this.runState?.pipe(map((s) => s.evaluationId))
    }
  public startTask() {
    this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskStart(id))).subscribe(
      (r) => {
        this.refreshSubject.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public nextTask() {
    this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskNext(id))).subscribe(
      (r) => {
        this.refreshSubject.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public previousTask() {
    this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskPrevious(id))).subscribe(
      (r) => {
        this.refreshSubject.next();
        this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
      },
      (r) => {
        this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
      }
    );
  }

  public abortTask() {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        text: 'Really end the task?',
        color: 'warn',
      } as ConfirmationDialogComponentData,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.runId.pipe(switchMap((id) => this.runAdminService.postApiV2EvaluationAdminByEvaluationIdTaskAbort(id))).subscribe(
          (r) => {
            this.refreshSubject.next();
            this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
          },
          (r) => {
            this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
          }
        );
      }
    });
  }

  public adjustDuration(duration: number) {
    this.runId
      .pipe(switchMap((id) => this.runAdminService.patchApiV2EvaluationAdminByEvaluationIdAdjustByDuration(id, duration)))
      .subscribe(
        (r) => {
          this.refreshSubject.next();
          this.snackBar.open(`Success: ${r.description}`, null, { duration: 5000 });
        },
        (r) => {
          this.snackBar.open(`Error: ${r.error.description}`, null, { duration: 5000 });
        }
      );
  }

}
