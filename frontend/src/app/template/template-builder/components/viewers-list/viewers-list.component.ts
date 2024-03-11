import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { ApiRole, ApiUser, TemplateService, UserService } from "../../../../../../openapi";
import { MatTable } from "@angular/material/table";
import { Observable } from "rxjs";
import { TemplateBuilderService } from "../../template-builder.service";
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";
import { map, shareReplay, tap } from "rxjs/operators";
import { MatAutocompleteSelectedEvent } from "@angular/material/autocomplete";

@Component({
  selector: "app-viewers-list",
  templateUrl: "./viewers-list.component.html",
  styleUrls: ["./viewers-list.component.scss"]
})
export class ViewersListComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {

  /** The table to use for the "list" */
  @ViewChild("table")
  table: MatTable<ApiUser>;

  /** The users that are available, i.e. other except those in the list */
  availableUsers: Observable<ApiUser[]>;

  /** The columns in the table */
  displayedColumns: string[] = ["name", "action"];

  /** The initially empty list of users in the list */
  users: Observable<Array<string>> = new Observable<Array<string>>((x) => x.next([]));

  constructor(
    private userService: UserService,
    builderService: TemplateBuilderService,
    route: ActivatedRoute,
    templateService: TemplateService,
    snackBar: MatSnackBar
  ) {
    super(builderService, route, templateService, snackBar);
    this.refreshAvailableUsers();
  }

  addUser(event: MatAutocompleteSelectedEvent){
    if(this.builderService.getTemplate().viewers.includes(event.option.value.id)){
      // We ignore a possible add when the user is already in the list
      return;
    }
    this.builderService.getTemplate().viewers.push(event.option.value.id);
    this.builderService.update();
    this.table.renderRows();
  }

  remove(userId: string){
    this.builderService.getTemplate().viewers.splice(this.builderService.getTemplate().viewers.indexOf(userId),1);
    this.builderService.update();
    this.table.renderRows();
  }

  userForId(id: string){
    return this.availableUsers.pipe(map((users) => users.find((u) => u.id === id)))
  }

  displayUser(user: ApiUser){
    return user.username
  }

  ngOnInit() {
    this.onInit()
  }

  ngOnDestroy() {
    this.onDestroy()
  }

  onChange() {
    this.users = this.builderService.templateAsObservable().pipe(
      map((t) => {
        if(t){
          return t.viewers;
        }else{
          return [];
        }
      }),
      tap(_ => this.table?.renderRows())
    )
  }

  refreshAvailableUsers(){
    this.availableUsers = this.userService.getApiV2UserList().pipe(
      map((users) => users.filter((user) => user.role === ApiRole.VIEWER)),
      shareReplay(1)
    )
  }
}
