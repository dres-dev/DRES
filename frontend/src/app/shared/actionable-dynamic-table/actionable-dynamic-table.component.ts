import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {MatTable} from '@angular/material/table';

export class ActionableDynamicTableColumnDefinition {
  property: string;
  type: ActionableDynamicTableColumnType;
  header: string;

  actions?: ActionableDynamicTableActionType[]
  /**
   * The name of the template to use in case this has the type CUSTOM
   */
  customName?: string;
}

export enum ActionableDynamicTableColumnType {
  TEXT = 'text',
  NUMBER = 'number',
  ACTION = 'action',
  CUSTOM = 'custom'
}

export enum ActionableDynamicTableActionType{
  EDIT= 'edit',
  REMOVE = 'remove'
}

export interface ActionableDynamicTableHandler<T>{
  add(): T;
  edit(obj: T);
  beforeRemove(obj: T): boolean;
}

@Component({
  selector: 'app-actionable-dynamic-table',
  templateUrl: './actionable-dynamic-table.component.html',
  styleUrls: ['./actionable-dynamic-table.component.scss']
})
export class ActionableDynamicTable<T> {

  @Input()
  public dataSource: Array<T>;

  @Input()
  public columnSchema: ActionableDynamicTableColumnDefinition[];

  @Input()
  public displayedColumns: string[];

  @Input()
  public title: string;

  @Input()
  public trackByProperty?: string;

  private handler: ActionableDynamicTableHandler<T>;

  @ViewChild('table')
  table: MatTable<T>

  constructor() { }

  public setHandler(handler: ActionableDynamicTableHandler<T>){
    this.handler = handler;
  }

  onEdit(element: T){
    if(this.handler){
      this.handler.edit(element);
    }
  }

  onAdd(){
    if(this.handler){
      const newElement = this.handler.add();
      this.dataSource.push(newElement);
    }
  }

  onRemove(element: T){
    if(this.handler){
      if(this.handler.beforeRemove(element)){
        this.dataSource.splice(this.dataSource.indexOf(element), 1);
      }
    }
  }


}
