import { AfterContentInit, Component, ContentChildren, Input, OnInit, QueryList, TrackByFunction, ViewChild } from "@angular/core";
import { MatColumnDef, MatTable } from "@angular/material/table";

export class ActionableDynamicTableColumnDefinition {
  key: string;
  header: string;
  type: ActionableDynamicTableColumnType;
  property?: string;

  actions?: ActionableDynamicTableActionType[]
}

export enum ActionableDynamicTableColumnType {
  TEXT = 'text',
  NUMBER = 'number',
  ACTION = 'action',
  CUSTOM = 'custom'
}

export enum ActionableDynamicTableActionType{
  EDIT= 'edit',
  REMOVE = 'remove',
  DOWNLOAD = 'download'
}

@Component({
  selector: 'app-actionable-dynamic-table',
  templateUrl: './actionable-dynamic-table.component.html',
  styleUrls: ['./actionable-dynamic-table.component.scss']
})
export class ActionableDynamicTable<T> implements AfterContentInit{

  @Input()
  public dataSource: Array<T>;

  @Input()
  public columnSchema: ActionableDynamicTableColumnDefinition[];

  @Input()
  public displayedColumns: string[];

  @Input()
  public tableTitle: string;

  @Input()
  public trackByProperty?: string;

  @Input()
  public addIcon = 'add';
  @Input()
  public removeIcon = 'delete';
  @Input()
  public editIcon = 'edit';
  @Input()
  public downloadIcon = 'cloud_download';

  @Input()
  public onEdit?: (element: T) => void;

  @Input()
  public onRemove?: (element: T) => void;

  @Input()
  public onDownload?: (element: T) => void;

  @Input()
  public trackedBy?: TrackByFunction<T>

  @ContentChildren(MatColumnDef)
  columnDefs: QueryList<MatColumnDef>

  @ViewChild('table', {static: true})
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

  download(element: T){
    if(this.onDownload){
      this.onDownload(element);
    }
  }

  public renderRows(){
    this.table.renderRows();
  }

  customColumns(): ActionableDynamicTableColumnDefinition[]{
    return this.columnSchema.filter(cs => cs.type === ActionableDynamicTableColumnType.CUSTOM)
  }

  nonCustomColumns(): ActionableDynamicTableColumnDefinition[]{
    return this.columnSchema.filter(cs => cs.type !== ActionableDynamicTableColumnType.CUSTOM)
}

  ngAfterContentInit(): void {
    this.columnDefs.forEach(cd => this.table.addColumnDef(cd));
    if(this.trackedBy){
      this.table.trackBy = this.trackedBy
    }
  }

}
