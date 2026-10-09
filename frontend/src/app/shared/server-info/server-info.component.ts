import { AfterViewInit, Component, inject, OnDestroy, OnInit } from '@angular/core';
import { AuthenticationService } from '../../services/session/authentication.sevice';
import { Subscription } from 'rxjs';
import { ApiRole, DresInfo, StatusService } from '../../../../openapi';
import { LogService } from '../../services/logging/log.service';
import { LOGGER_CONFIG } from '../../services/logging/logger-config.token';
import { LoggerConfig } from '../../services/logging/logger.config';

@Component({
  selector: 'app-server-info',
  templateUrl: './server-info.component.html',
  styleUrls: ['./server-info.component.scss'],
  providers: [LogService, { provide: LOGGER_CONFIG, useValue: { identifier: 'ServerInfoComponent' } as LoggerConfig }],
  standalone: false,
})
export class ServerInfoComponent implements OnInit, OnDestroy, AfterViewInit {
  private readonly LOGGER: LogService = inject<LogService>(LogService);

  public info: DresInfo | null = null;
  public isAdmin = false;
  public loadError = false;
  public loaded = false;

  private readonly authSub: Subscription;
  private infoSub: Subscription | null = null;

  constructor(
    private auth: AuthenticationService,
    private status: StatusService
  ) {
    this.authSub = this.auth.user.subscribe((u) => {
      this.isAdmin = u?.role === ApiRole.ADMIN;
    });
  }

  ngOnInit(): void {
    this.infoSub = this.status.getApiV2StatusInfo().subscribe({
      next: (info) => {
        this.info = info;
        this.loaded = true;
      },
      error: () => {
        this.loadError = true;
        this.loaded = true;
      },
    });
  }

  ngAfterViewInit() {
    this.LOGGER.info('Hi');
  }

  ngOnDestroy(): void {
    this.infoSub?.unsubscribe();
    this.infoSub = null;
    this.authSub.unsubscribe();
  }
}
