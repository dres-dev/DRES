import { AfterViewInit, Component, OnDestroy, OnInit } from "@angular/core";
import { AuthenticationService } from "../../services/session/authentication.sevice";
import { Observable, Subscription } from "rxjs";
import { ApiRole, DresInfo, StatusService } from "../../../../openapi";

@Component({
  selector: "app-server-info",
  templateUrl: "./server-info.component.html",
  styleUrls: ["./server-info.component.scss"]
})
export class ServerInfoComponent implements OnInit, OnDestroy, AfterViewInit {

  public info: Observable<DresInfo>;

  private authSub: Subscription;
  private infoSub: Subscription;
  public isAdmin: boolean;


  constructor(private auth: AuthenticationService, private status: StatusService) {
    this.authSub = this.auth.user.subscribe(
      u => {
        this.isAdmin = u?.role === ApiRole.ADMIN
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

  }

  ngOnDestroy(): void {
    this.infoSub?.unsubscribe();
    this.infoSub = null;
    this.authSub?.unsubscribe();
    this.authSub = null;
  }

}
