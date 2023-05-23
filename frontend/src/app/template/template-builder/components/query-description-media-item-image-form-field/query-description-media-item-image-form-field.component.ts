import { Component, Input } from "@angular/core";
import { UntypedFormControl } from "@angular/forms";
import {
  CompetitionFormBuilder
} from "../../../../competition/competition-builder/competition-builder-task-dialog/competition-form.builder";
import { ApiMediaItem } from "../../../../../../openapi";
import { FormatMediaItemPipe, MediaItemDisplayOptions } from "../../../../services/pipes/format-media-item.pipe";
import {
  QueryDescriptionMediaItemFormFieldComponent
} from "../query-description-media-item-form-field/query-description-media-item-form-field.component";

@Component({
  selector: 'app-query-description-media-item-image-form-field',
  templateUrl: './query-description-media-item-image-form-field.component.html',
  styleUrls: ['./query-description-media-item-image-form-field.component.scss']
})
export class QueryDescriptionMediaItemImageFormFieldComponent extends QueryDescriptionMediaItemFormFieldComponent{



}
