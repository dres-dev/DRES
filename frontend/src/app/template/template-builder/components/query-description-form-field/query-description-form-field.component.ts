import { Component, Input } from "@angular/core";
import { AbstractControl, UntypedFormControl, UntypedFormGroup } from "@angular/forms";
import {
  CompetitionFormBuilder
} from "../../../../competition/competition-builder/competition-builder-task-dialog/competition-form.builder";
import { ApiHintOption, ApiHintType } from "../../../../../../openapi";

@Component({
  selector: 'app-query-description-form-field',
  templateUrl: './query-description-form-field.component.html',
  styleUrls: ['./query-description-form-field.component.scss']
})
export class QueryDescriptionFormFieldComponent {

  @Input()
  startControl: UntypedFormControl
  @Input()
  endControl: UntypedFormControl
  @Input()
  typeControl: UntypedFormControl
  @Input()
  externalControl: UntypedFormControl
  @Input()
  itemControl: UntypedFormControl
  @Input()
  descriptionControl: UntypedFormControl
  @Input()
  pathControl: UntypedFormControl
  @Input()
  segmentStartControl: UntypedFormControl
  @Input()
  segmentEndControl: UntypedFormControl
  @Input()
  unitControl: UntypedFormControl
  @Input()
  formBuilder: CompetitionFormBuilder
  @Input()
  index: number

  public addQueryComponent(componentType: ApiHintOption, previous: number = null){
    switch(componentType){
      case 'IMAGE_ITEM':
        this.formBuilder.addComponentForm(ApiHintType.IMAGE, previous);
        break;
      case 'VIDEO_ITEM_SEGMENT':
        this.formBuilder.addComponentForm(ApiHintType.VIDEO, previous);
        break;
      case 'TEXT':
        this.formBuilder.addComponentForm(ApiHintType.TEXT, previous);
        break;
      case 'EXTERNAL_IMAGE':
        this.formBuilder.addComponentForm(ApiHintType.IMAGE, previous, true);
        break;
      case 'EXTERNAL_VIDEO':
        this.formBuilder.addComponentForm(ApiHintType.VIDEO, previous, true);
        break;
    }
  }

  public removeQueryComponent(index: number) {
    this.formBuilder.removeComponentForm(index);
  }
}
