import { Component, Injector, Input } from "@angular/core";
import { UntypedFormControl } from "@angular/forms";
import {
  TaskTemplateFormBuilder
} from "../../task-template-form.builder";
import { FormatMediaItemPipe, MediaItemDisplayOptions } from "../../../../services/pipes/format-media-item.pipe";
import { ApiMediaItem } from "../../../../../../openapi";

@Component({
  selector: "app-query-description-media-item-form-field",
  templateUrl: "./query-description-media-item-form-field.component.html",
  styleUrls: ["./query-description-media-item-form-field.component.scss"]
})
export class QueryDescriptionMediaItemFormFieldComponent {
  @Input()
  itemControl: UntypedFormControl;
  @Input()
  formBuilder: TaskTemplateFormBuilder;
  @Input()
  index: number;

  showing: boolean;

  protected formatMediaItemPipe: FormatMediaItemPipe;

  constructor(injector: Injector) {
    this.formatMediaItemPipe = injector.get(FormatMediaItemPipe);
  }

  public displayWithMediaItem(value: ApiMediaItem): string {
    if(value){
      if (this.formatMediaItemPipe) {
        return this.formatMediaItemPipe.transform(value, { showType: true } as MediaItemDisplayOptions);
      } else {
        return `${value.name} (${value.type})`;
      }
    }else{
      return '';
    }
  }
}
