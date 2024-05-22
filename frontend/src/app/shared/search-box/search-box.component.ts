import { Component, EventEmitter, OnInit, Output } from "@angular/core";

@Component({
  selector: 'app-search-box',
  templateUrl: './search-box.component.html',
  styleUrls: ['./search-box.component.scss']
})
export class SearchBoxComponent{
  // Source: https://angular-htpgvx.stackblitz.io
  @Output() filterChanged = new EventEmitter<string>();
  public searchBoxActive = false;
  filter: string;

  onFilterClear(){
    this.filter = '';
    this.searchBoxActive = false;
    this.filterChanged.emit(this.filter);
  }

  onTextChanged(){
    this.filterChanged.emit(this.filter);
  }

  public clear(){
    this.onFilterClear();
  }
}
