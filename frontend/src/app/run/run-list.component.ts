import { Component, ViewChild } from "@angular/core";
import { Observable } from 'rxjs';
import { AuthenticationService } from '../services/session/authentication.sevice';
import { map } from 'rxjs/operators';
import { AccessChecking } from '../model/access-checking.interface';
import { UserGroup } from '../model/user-group.model';
import { AccessRoleService } from '../services/session/access-role.service';
import {ApiRole} from '../../../openapi';
import { AdminRunListComponent } from "./admin-run-list.component";
import { ViewerRunListComponent } from "./viewer-run-list.component";

@Component({
  selector: 'app-run-list',
  templateUrl: './run-list.component.html',
  styleUrls: ['./run-list.component.scss']
})
export class RunListComponent implements AccessChecking {
  currentRole: Observable<ApiRole>;

  adminGroup = AccessRoleService.ADMIN_GROUP;

  @ViewChild('runList') adminList: AdminRunListComponent
  @ViewChild('viewer') normalList: ViewerRunListComponent

  constructor(private authenticationService: AuthenticationService, private accessService: AccessRoleService) {
    this.currentRole = this.authenticationService.user.pipe(map((u) => u.role));
  }

  refresh(){
    if(this.adminList instanceof  AdminRunListComponent){
      this.adminList?.refresh();
    }else if(this.normalList instanceof  ViewerRunListComponent){
      this.normalList?.refresh();
    }
  }

  hasAccessFor(group: UserGroup): boolean {
    return this.accessService.accessGranted(group);
  }
}
