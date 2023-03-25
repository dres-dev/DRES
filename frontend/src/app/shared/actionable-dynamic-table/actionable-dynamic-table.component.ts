import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {MatTable} from '@angular/material/table';

export class ActionableDynamicTableColumnDefinition {
  key: string;
  header: string;
  type: ActionableDynamicTableColumnType;
  property?: string;

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

  @Input()
  public onEdit?: (element: T) => void;

  @Input()
  public onRemove?: (element: T) => void;



  @ViewChild('table')
  table: MatTable<T>

  constructor() { }

  edit(element: T){
    if(this.onEdit){
      this.onEdit(element);
      this.table.renderRows();
    }
  }


  remove(element: T){
    if(this.onRemove){
      this.onRemove(element);
      this.table.renderRows();
    }
  }

  public renderRows(){
    this.table.renderRows();
  }


}
