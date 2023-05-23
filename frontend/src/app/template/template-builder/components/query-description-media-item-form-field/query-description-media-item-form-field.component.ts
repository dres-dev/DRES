import { Component, Input } from "@angular/core";
import { UntypedFormControl } from "@angular/forms";
import {
  CompetitionFormBuilder
} from "../../../../competition/competition-builder/competition-builder-task-dialog/competition-form.builder";
import { FormatMediaItemPipe, MediaItemDisplayOptions } from "../../../../services/pipes/format-media-item.pipe";
import { ApiMediaItem } from "../../../../../../openapi";

@Component({
  selector: 'app-query-description-media-item-form-field',
  templateUrl: './query-description-media-item-form-field.component.html',
  styleUrls: ['./query-description-media-item-form-field.component.scss']
})
export class QueryDescriptionMediaItemFormFieldComponent {
  @Input()
  itemControl: UntypedFormControl
  @Input()
  formBuilder: CompetitionFormBuilder
  @Input()
  index: number

  showing: boolean;

  constructor(private formatMediaItem: FormatMediaItemPipe) {
  }

  public displayWithMediaItem(value: ApiMediaItem): string{
    return this.formatMediaItem.transform(value, {showType: true} as MediaItemDisplayOptions)
  }
}
