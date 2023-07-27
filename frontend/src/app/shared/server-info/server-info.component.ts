import { AfterViewInit, Component, inject, OnDestroy, OnInit } from "@angular/core";
import { AuthenticationService } from "../../services/session/authentication.sevice";
import { Observable, Subscription } from "rxjs";
import { ApiRole, DresInfo, StatusService } from "../../../../openapi";
import { LogService } from "../../services/logging/log.service";
import { LOGGER_CONFIG } from "../../services/logging/logger-config.token";
import { LoggerConfig } from "../../services/logging/logger.config";

@Component({
  selector: "app-server-info",
  templateUrl: "./server-info.component.html",
  styleUrls: ["./server-info.component.scss"],
  providers:[LogService, {provide: LOGGER_CONFIG, useValue: {identifier: 'ServerInfoComponent'} as LoggerConfig}]
})
export class ServerInfoComponent implements OnInit, OnDestroy, AfterViewInit {

  private readonly LOGGER: LogService = inject<LogService>(LogService)

  public info: Observable<DresInfo>;

  private authSub: Subscription;
  private infoSub: Subscription;
  public isAdmin: boolean;


  constructor(private auth: AuthenticationService, private status: StatusService) {
    this.authSub = this.auth.user.subscribe(
      u => {
        this.isAdmin = u?.role === ApiRole.ADMIN;
      }
    );
  }

  ngOnInit(): void {
    this.infoSub = this.status.getApiV2StatusInfo().subscribe(info => {
      this.info = new Observable<DresInfo>(subscriber => {
        subscriber.next(info);
      });
    });
  }

  ngAfterViewInit() {
    this.LOGGER.info("Hi")
  }

  ngOnDestroy(): void {
    this.infoSub?.unsubscribe();
    this.infoSub = null;
    this.authSub?.unsubscribe();
    this.authSub = null;
  }

}
