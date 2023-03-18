import {AfterViewInit, Component, Inject, OnDestroy, OnInit, TemplateRef, ViewChild, ViewContainerRef} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {BehaviorSubject, interval, merge, Observable, of, Subscription, zip} from 'rxjs';
import {
  catchError,
  delay,
  filter,
  flatMap,
  map,
  pairwise,
  retryWhen,
  sampleTime,
  share,
  shareReplay,
  switchMap,
  tap,
  withLatestFrom,
} from 'rxjs/operators';
import { webSocket, WebSocketSubject, WebSocketSubjectConfig } from 'rxjs/webSocket';
import { AppConfig } from '../app.config';
import { IWsMessage } from '../model/ws/ws-message.interface';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { IWsClientMessage } from '../model/ws/ws-client-message.interface';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Position } from './model/run-viewer-position';
import { Widget } from './model/run-viewer-widgets';
import { DOCUMENT } from '@angular/common';
import {Title} from '@angular/platform-browser';
import {ApiEvaluationInfo, ApiEvaluationState, ApiTaskTemplateInfo, EvaluationService} from '../../../openapi';
import {Overlay, OverlayRef} from "@angular/cdk/overlay";
import {TemplatePortal} from "@angular/cdk/portal";

@Component({
  selector: 'app-run-viewer',
  templateUrl: './run-viewer.component.html',
  styleUrls: ['./run-viewer.component.scss']
})
export class RunViewerComponent implements OnInit, AfterViewInit, OnDestroy {
  /** The WebSocketSubject that represent the WebSocket connection to the DRES endpoint. */
  webSocketSubject: WebSocketSubject<IWsMessage>;

  /** A {@link BehaviorSubject} that reflect the WebSocket connection status. */
  webSocketConnectionSubject: BehaviorSubject<boolean> = new BehaviorSubject(false);

  /** Observable for incoming WebSocket messages. */
  webSocket: Observable<IWsServerMessage>;

  /** Observable for current run ID. */
  evaluationId: Observable<string>;

  /** Observable for information about the current run. Usually queried once when the view is loaded. */
  runInfo: Observable<ApiEvaluationInfo>;

  /** Observable for information about the current run's state. Usually queried when a state change is signaled via WebSocket. */
  runState: Observable<ApiEvaluationState>;

  /** Observable that fires whenever a task starts. Emits the task description of the task that just started. */
  taskStarted: Observable<ApiTaskTemplateInfo>;

  /** Observable that fires whenever a task changes. Emits the task description of the new task. */
  taskChanged: Observable<ApiTaskTemplateInfo>;

  /** Observable that fires whenever a task ends. Emits the task description of the task that just ended. */
  taskEnded: Observable<ApiTaskTemplateInfo>;

  /** Observable of the {@link Widget} that should be displayed on the left-hand side. */
  leftWidget: Observable<Widget>;

  /** Observable of the {@link Widget} that should be displayed on the right-hand side. */
  rightWidget: Observable<Widget>;

  /** Observable of the {@link Widget} that should be displayed at the center. */
  centerWidget: Observable<Widget>;

  /** Observable of the {@link Widget} that should be displayed at the bottom. */
  bottomWidget: Observable<Widget>;

  /** Reference to the {@link OverlayRef}. Initialized in ngAfterViewInit(). */
  private overlayRef: OverlayRef = null

  /** Reference to the overlay template. Only available once view has been loaded. */
  @ViewChild('overlayTemplate')
  private overlayTemplateRef: TemplateRef<unknown>;

  /** Reference to the overlay template. Only available once view has been loaded. */
  private overlaySubscription: Subscription;


  /** Internal WebSocket subscription for pinging the server. */
  private pingSubscription: Subscription;

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
    /** Initialize basic WebSocketSubject. */
    const wsurl = this.config.webSocketUrl;
    const connectionSubject = this.webSocketConnectionSubject
    this.webSocketSubject = webSocket({
      url: wsurl,
      openObserver: {
        next(openEvent) {
          console.log(`[RunViewerComponent] WebSocket connection to ${wsurl} established!`);
          connectionSubject.next(true)
        },
      },
      closeObserver: {
        next(closeEvent: CloseEvent) {
          console.log(`[RunViewerComponent] WebSocket connection to ${wsurl} closed: ${closeEvent.reason}.`);
          connectionSubject.next(false)
        },
      },
    } as WebSocketSubjectConfig<IWsMessage>);

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

    /* Basic observable for general run info; this information is static and does not change over the course of a run. */
    this.runInfo = this.evaluationId.pipe(
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

    /* Basic observable for web socket messages received from the DRES server. */
    this.webSocket = this.evaluationId.pipe(
      flatMap((evaluationId) =>
        this.webSocketSubject.multiplex(
            () => {
              return { evaluationId: evaluationId, type: 'REGISTER' } as IWsClientMessage;
            },
            () => {
              return { evaluationId: evaluationId, type: 'UNREGISTER' } as IWsClientMessage;
            },
            (message) => message.evaluationId === evaluationId || message.evaluationId === null
          )
          .pipe(
            retryWhen((err) =>
              err.pipe(
                tap((e) =>
                  console.error(
                    '[RunViewerComponent] An error occurred with the WebSocket communication channel. Trying to reconnect in 1 second.',
                    e
                  )
                ),
                delay(5000)
              )
            ),
            map((m) => m as IWsServerMessage),
            filter((q) => q != null),
            tap((m) => console.log(`[RunViewerComponent] WebSocket message received: ${m.type}`))
          )
      ),
      share()
    );

    /*
     * Observable for run state info; this information is dynamic and is subject to change over the course of a run.
     *
     * Updates to the RunState are triggered by WebSocket messages received by the viewer. To not overwhelm the server,
     * the RunState is updated every 500ms at most.
     */
    const wsMessages = this.webSocket.pipe(
      filter((m) => m.type !== 'PING') /* Filter out ping messages. */,
      map((b) => b.evaluationId)
    );
    this.runState = merge(this.evaluationId, wsMessages).pipe(
      sampleTime(500) /* State updates are triggered only once every 500ms. */,
      switchMap((evaluationId) =>
        this.runService.getApiV2EvaluationByEvaluationIdState(evaluationId).pipe(
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
          filter((q) => q != null)
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /* Basic observable that fires when a task starts.  */
    this.taskStarted = this.runState.pipe(
      pairwise(),
      filter(([s1, s2]) => (s1 === null || s1.taskStatus === 'PREPARING') && s2.taskStatus === 'RUNNING'),
      map(([s1, s2]) => s2.currentTemplate),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /* Basic observable that fires when a task ends.  */
    this.taskEnded = merge(of(null as ApiEvaluationState), this.runState).pipe(
      pairwise(),
      filter(([s1, s2]) => (s1 === null || s1.taskStatus === 'RUNNING') && s2.taskStatus === 'ENDED'),
      map(([s1, s2]) => s2.currentTemplate),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    /* Observable that tracks the currently active task. */
    this.taskChanged = merge(of(null as ApiEvaluationState), this.runState).pipe(
      pairwise(),
      filter(([s1, s2]) => s1 === null || s1.currentTemplate.name !== s2.currentTemplate.name),
      map(([s1, s2]) => s2.currentTemplate),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    this.runInfo.subscribe((info: ApiEvaluationInfo) => {
        this.titleService.setTitle(info.name + ' - DRES');
    })
  }

  /**
   * Registers this RunViewerComponent on view initialization and creates the WebSocket subscription.
   */
  ngOnInit(): void {
    /* Register WebSocket ping. */
    this.pingSubscription = interval(5000)
      .pipe(
        withLatestFrom(this.evaluationId),
        tap(([i, evaluationId]) => this.webSocketSubject.next({ evaluationId: evaluationId, type: 'PING' } as IWsClientMessage))
      )
      .subscribe();

  }

  /**
   * Prepare the overlay that is being displayed when WebSocket connection times out.
   */
  ngAfterViewInit() {
    this.overlayRef = this.overlay.create({
      positionStrategy: this.overlay.position().global().centerHorizontally().centerVertically(),
      scrollStrategy: this.overlay.scrollStrategies.block(),
      backdropClass: "overlay",
      hasBackdrop: true
    });

    /* Create subscription for WebSocket connection status and show overlay. */
    this.overlaySubscription = this.webSocketConnectionSubject.subscribe((status) => {
      if (status == false) {
        this.overlayRef.attach(new TemplatePortal(this.overlayTemplateRef, this._viewContainerRef))
      } else {
        this.overlayRef.detach()
      }
    })
  }

    /**
   * Unregisters this RunViewerComponent on view destruction and cleans the WebSocket subscription.
   */
  ngOnDestroy(): void {
    /* Unregister Ping service. */
    this.pingSubscription.unsubscribe();
    this.pingSubscription = null;
    this.overlaySubscription.unsubscribe()
    this.overlaySubscription = null
    this.overlayRef?.dispose()
    this.overlayRef = null
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
