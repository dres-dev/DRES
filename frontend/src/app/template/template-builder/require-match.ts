import { AbstractControl } from '@angular/forms';

/**
 * https://onthecode.co.uk/force-selection-angular-material-autocomplete/
 * @param control
 * @constructor
 */
export function RequireMatch(control: AbstractControl) {
  const selection: any = control.value;
  if (typeof selection === 'string') {
    return { incorrect: true };
  }
  return null;
}
