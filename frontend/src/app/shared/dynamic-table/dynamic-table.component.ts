import {Component, Input} from '@angular/core';

export interface ColumnDefinition {
  property: string,
  type: string, // TODO enumize it
  header: string
}

@Component({
  selector: 'app-dynamic-table',
  templateUrl: './dynamic-table.component.html',
  styleUrls: ['./dynamic-table.component.scss']
})
export class DynamicTableComponent<T> {

  @Input()
  public dataSource: Array<T>;

  @Input()
  public columnSchema: ColumnDefinition[];

  @Input()
  public displayedColumns: string[];

  constructor() { }

}
