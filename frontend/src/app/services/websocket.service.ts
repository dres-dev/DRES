import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, Subscription, interval } from 'rxjs';
import { AppConfig } from '../app.config';
import { IWsServerMessage } from '../model/ws/ws-server-message.interface';
import { IWsClientMessage } from '../model/ws/ws-client-message.interface';
import { ClientMessageType } from '../model/ws/client-message-type.enum';

@Injectable({
  providedIn: 'root',
})
export class WebSocketService implements OnDestroy {
  private socket: WebSocket | null = null;
  private messageSubject = new Subject<IWsServerMessage>();
  private pingSubscription: Subscription | null = null;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private currentEvaluationId: string | null = null;

  /** Observable stream of messages pushed by the server. */
  readonly messages$: Observable<IWsServerMessage> = this.messageSubject.asObservable();

  constructor(private config: AppConfig) {}

  /**
   * Connects to the WebSocket endpoint and registers for the given evaluation.
   * Safe to call multiple times — no-ops if already connected to the same evaluation.
   */
  connect(evaluationId: string): void {
    if (this.socket?.readyState === WebSocket.OPEN && this.currentEvaluationId === evaluationId) {
      return;
    }
    this.closeSocket();
    this.currentEvaluationId = evaluationId;
    this.openConnection();
  }

  /** Disconnects from the WebSocket endpoint and stops reconnect attempts. */
  disconnect(): void {
    this.currentEvaluationId = null;
    this.closeSocket();
  }

  private openConnection(): void {
    const url = this.config.webSocketUrl;
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      this.send({
        evaluationId: this.currentEvaluationId,
        type: ClientMessageType.ClientMessageTypeEnum.REGISTER,
      });
      this.startPing();
    };

    this.socket.onmessage = (event: MessageEvent) => {
      try {
        const message: IWsServerMessage = JSON.parse(event.data as string);
        this.messageSubject.next(message);
      } catch {
        console.warn('[WebSocketService] Could not parse message:', event.data);
      }
    };

    this.socket.onclose = () => {
      this.stopPing();
      if (this.currentEvaluationId) {
        this.reconnectTimeout = setTimeout(() => this.openConnection(), 5000);
      }
    };

    this.socket.onerror = (error: Event) => {
      console.error('[WebSocketService] WebSocket error:', error);
    };
  }

  private send(message: IWsClientMessage): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    }
  }

  private closeSocket(): void {
    if (this.reconnectTimeout !== null) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    this.stopPing();
    if (this.socket) {
      if (this.socket.readyState === WebSocket.OPEN) {
        this.send({
          evaluationId: this.currentEvaluationId,
          type: ClientMessageType.ClientMessageTypeEnum.UNREGISTER,
        });
        this.socket.close();
      }
      this.socket = null;
    }
  }

  private startPing(): void {
    this.pingSubscription = interval(30_000).subscribe(() => {
      if (this.currentEvaluationId) {
        this.send({
          evaluationId: this.currentEvaluationId,
          type: ClientMessageType.ClientMessageTypeEnum.PING,
        });
      }
    });
  }

  private stopPing(): void {
    this.pingSubscription?.unsubscribe();
    this.pingSubscription = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.messageSubject.complete();
  }
}
