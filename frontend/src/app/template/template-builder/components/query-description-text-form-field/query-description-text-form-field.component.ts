import { Component, Input } from "@angular/core";
import { UntypedFormControl } from "@angular/forms";

@Component({
  selector: 'app-query-description-text-form-field',
  templateUrl: './query-description-text-form-field.component.html',
  styleUrls: ['./query-description-text-form-field.component.scss']
})
export class QueryDescriptionTextFormFieldComponent {

  @Input()
  control: UntypedFormControl

}
