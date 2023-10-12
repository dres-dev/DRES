import { Component, Injector, Input } from "@angular/core";
import {
  QueryDescriptionMediaItemFormFieldComponent
} from "../query-description-media-item-form-field/query-description-media-item-form-field.component";

@Component({
  selector: 'app-query-description-media-item-image-form-field',
  templateUrl: './query-description-media-item-image-form-field.component.html',
  styleUrls: ['./query-description-media-item-image-form-field.component.scss']
})
export class QueryDescriptionMediaItemImageFormFieldComponent extends QueryDescriptionMediaItemFormFieldComponent{

  constructor(injector: Injector) {
    super(injector)
  }



}
