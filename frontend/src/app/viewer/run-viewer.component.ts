import {AfterViewInit, Component, Inject, OnDestroy, OnInit, ViewContainerRef} from '@angular/core';
import { ActivatedRoute, ActivationEnd, Params, Router } from "@angular/router";
import {interval, merge, mergeMap, Observable, of, zip} from 'rxjs';
import {
  catchError,
  filter,
  map,
  pairwise,
  shareReplay,
  switchMap, tap
} from "rxjs/operators";
import { AppConfig } from '../app.config';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Position } from './model/run-viewer-position';
import { Widget } from './model/run-viewer-widgets';
import { DOCUMENT } from '@angular/common';
import {Title} from '@angular/platform-browser';
import {ApiEvaluationInfo, ApiEvaluationState, EvaluationService} from '../../../openapi';
import {Overlay} from "@angular/cdk/overlay";

@Component({
  selector: 'app-run-viewer',
  templateUrl: './run-viewer.component.html',
  styleUrls: ['./run-viewer.component.scss']
})
export class RunViewerComponent implements OnInit, AfterViewInit, OnDestroy {

  /** Observable for current run ID. */
  evaluationId: Observable<string>;

  /** Observable for information about the current run. Usually queried once when the view is loaded. */
  info: Observable<ApiEvaluationInfo>;

  /** Observable for information about the current run's {@link ApiEvaluationState}. */
  state: Observable<ApiEvaluationState>;

  /** Observable that fires whenever a task starts. Emits the {@link ApiEvaluationState} that triggered the fire. */
  taskStarted: Observable<ApiEvaluationState>;

  /** Observable that fires whenever a task ends. Emits the {@link ApiEvaluationState} that triggered the fire. */
  taskEnded: Observable<ApiEvaluationState>;

  /** Observable that fires whenever the active task template changes. Emits the {@link ApiEvaluationState} that triggered the fire. */
  taskChanged: Observable<ApiEvaluationState>;

  /** Observable of the {@link Widget} that should be displayed on the left-hand side. */
  leftWidget: Observable<Widget>;

  /** Observable of the {@link Widget} that should be displayed on the right-hand side. */
  rightWidget: Observable<Widget>;

  /** Observable of the {@link Widget} that should be displayed at the center. */
  centerWidget: Observable<Widget>;

  /** Observable of the {@link Widget} that should be displayed at the bottom. */
  bottomWidget: Observable<Widget>;

  noUi: Observable<boolean>;

  /** Cached config */
  private p: any;

  /**
   * Constructor; extracts the runId and keeps a local reference.
   */
  constructor(
    private router: Router,
    private activeRoute: ActivatedRoute,
    private config: AppConfig,
    private runService: EvaluationService,
    private snackBar: MatSnackBar,
    private titleService: Title,
    private overlay: Overlay,
    @Inject(DOCUMENT) private document: Document,
    private _viewContainerRef: ViewContainerRef
  ) {

    /** Observable for the current run ID. */
    this.evaluationId = this.activeRoute.params.pipe(
      map((a) => {
        /* A hack since our custom url serializer kicks in too late */
        if (a.runId.includes(';')) {
          return a.runId.split(';')[0];
        } else if (a.runId.includes('%')) {
          return a.runId.split('%')[0];
        } else {
          return a.runId;
        }
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /** Observable for the currently selected Widget. Also sets reasonable defaults */
    this.centerWidget = this.activeRoute.paramMap.pipe(
      map((a) => this.resolveWidgetFromParams(a, 'center')),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.leftWidget = this.activeRoute.paramMap.pipe(
      map((a) => this.resolveWidgetFromParams(a, 'left')),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.rightWidget = this.activeRoute.paramMap.pipe(
      map((a) => this.resolveWidgetFromParams(a, 'right')),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.bottomWidget = this.activeRoute.paramMap.pipe(
      map((a) => this.resolveWidgetFromParams(a, 'bottom')),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.noUi = this.activeRoute.paramMap.pipe(
      map((a) => {
        console.log("A", a);
        const map = this.parseMatrixParams(a.get('runId'))
        return Object.keys(map).includes("noUi") && map['noUi'] === "true"
      })
    )

    /* Basic observable for general run info; this information is static and does not change over the course of a run. */
    this.info = this.evaluationId.pipe(
      switchMap((evaluationId) =>
        this.runService.getApiV2EvaluationByEvaluationIdInfo(evaluationId).pipe(
          catchError((err, o) => {
            console.log(
              `[RunViewerComponent] There was an error while loading information in the current run: ${err?.message}`
            );
            this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`, null, {
              duration: 5000,
            });
            if (err.status === 404) {
              this.router.navigate(['/template/list']);
            }
            return of(null);
          }),
          filter((q) => q != null)
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    this.state = interval(1000).pipe(mergeMap(() => this.evaluationId)).pipe(
      switchMap((id) => this.runService.getApiV2EvaluationByEvaluationIdState(id)),
      catchError((err, o) => {
        console.log(
          `[RunViewerComponent] There was an error while loading information in the current run state: ${err?.message}`
        );
        this.snackBar.open(`There was an error while loading information in the current run: ${err?.message}`, null, {
          duration: 5000,
        });
        if (err.status === 404) {
          this.router.navigate(['/template/list']);
        }
        return of(null);
      }),
      filter((q) => q != null),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /* Basic observable that fires when a task starts.  */
    this.taskStarted = merge(of(null as ApiEvaluationState), this.state).pipe(
      pairwise(),
      filter(([s1, s2]) => (s1 === null || s1.taskStatus === 'PREPARING') && s2.taskStatus === 'RUNNING'),
      map(([s1, s2]) => s2),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /* Basic observable that fires when a task ends.  */
    this.taskEnded = merge(of(null as ApiEvaluationState), this.state).pipe(
      pairwise(),
      filter(([s1, s2]) => (s1 === null || s1.taskStatus === 'RUNNING') && s2.taskStatus === 'ENDED'),
      map(([s1, s2]) => s2),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /* Observable that tracks the currently active task. */
    this.taskChanged = merge(of(null as ApiEvaluationState), this.state).pipe(
      pairwise(),
      filter(([s1, s2]) => s1 === null || s1.taskTemplateId !== s2.taskTemplateId),
      map(([s1, s2]) => s2),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    this.info.subscribe((info: ApiEvaluationInfo) => {
        this.titleService.setTitle(info.name + ' - DRES');
    })
  }

  /**
   * Registers this RunViewerComponent on view initialization and creates the WebSocket subscription.
   */
  ngOnInit(): void {
    
  }

  /**
   * Prepare the overlay that is being displayed when WebSocket connection times out.
   */
  ngAfterViewInit() {
    

  }

    /**
   * Unregisters this RunViewerComponent on view destruction and cleans the WebSocket subscription.
   */
  ngOnDestroy(): void {
    this.titleService.setTitle('DRES');
  }

  /**
   * Updates the {@link Widget} for the specified position.
   *
   * @param position The {@link Position} to update.
   * @param widget The name of the new {@link Widget}.
   */
  public updateWidgetForPosition(position: string, widget: string) {
    const pCopy = {
      left: this.p.left,
      right: this.p.right,
      center: this.p.center,
      bottom: this.p.bottom,
    };
    pCopy[position] = widget;
    this.router.navigate([this.router.url.substring(0, this.router.url.indexOf(';')), pCopy]);
  }

  /**
   * Determines and returns the number of body {@link Widget}s.
   *
   * @return Observable of the number of body {@link Widget}s.
   */
  public numberOfBodyWidgets(): Observable<number> {
    return zip(this.leftWidget, this.centerWidget, this.rightWidget).pipe(
      map(([l, c, r]) => {
        let n = 0;
        if (l) {
          n += 1;
        }
        if (c) {
          n += 1;
        }
        if (r) {
          n += 1;
        }
        return n;
      })
    );
  }

  public leftWidgetWidth(): Observable<string> {
    return zip(this.leftWidget, this.centerWidget, this.rightWidget).pipe(
      map(([l, c, r]) => {
        if (!l) {
          return '0%';
        }
        if (c) {
          if (r) {
            return '25%';
          }
          return '33%';
        }
        if (r) {
          return '49%';
        }
        return '100%';
      })
    );
  }

  public rightWidgetWidth(): Observable<string> {
    return zip(this.leftWidget, this.centerWidget, this.rightWidget).pipe(
      map(([l, c, r]) => {
        if (!r) {
          return '0%';
        }
        if (c) {
          if (l) {
            return '25%';
          }
          return '33%';
        }
        if (l) {
          return '49%';
        }
        return '100%';
      })
    );
  }

  public centerWidgetWidth(): Observable<string> {
    return zip(this.leftWidget, this.centerWidget, this.rightWidget).pipe(
      map(([l, c, r]) => {
        if (!c) {
          return '0%';
        }
        if (l) {
          if (r) {
            return '49%';
          }
          return '65%';
        }
        if (r) {
          return '50%';
        }
        return '100%';
      })
    );
  }

  /**
   * Returns a list of all available {@link Widget}s for the specified {@link Position}.
   *
   * @param position String representation of the {@link Position}.
   * @return Array of {@link Widget}s
   */
  public widgetsForPosition(position: string): Array<Widget> {
    switch (Position[position]) {
      case Position.LEFT:
      case Position.RIGHT:
      case Position.CENTER:
        return Widget.CENTER_WIDGETS;
      case Position.BOTTOM:
        return Widget.BOTTOM_WIDGETS;
      default:
        return [];
    }
  }

  private resolveWidgetFromParams(params: Params, position: string): Widget {
    if (params?.params?.runId?.includes(';')) {
      // We are in the case of broken url (i.e. reload)
      this.p = this.parseMatrixParams(params.params.runId);
    } else {
      // first time load, all fine
      this.p = params.params;
    }
    const w = Widget.resolveWidget(this.p[position], position);
    //console.log(`Position ${this.p[position]}/${position} resolved to `, w);
    return w;
  }

  /**
   * Since angular breaks (for some reason), the matrix param parsing is replicated here
   */
  private parseMatrixParams(str: string) {
    const paramMap = {};
    const parts = str.split(';');
    for (const part of parts) {
      const [key, value] = part.split('=');
      paramMap[key] = value;
    }
    return paramMap;
  }
}
