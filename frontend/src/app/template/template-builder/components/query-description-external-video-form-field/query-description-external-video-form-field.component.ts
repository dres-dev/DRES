import { Component, Input } from "@angular/core";
import {
  QueryDescriptionExternalFormFieldComponent
} from "../query-description-external-form-field/query-description-external-form-field.component";
import { UntypedFormControl } from "@angular/forms";
import { ApiTemporalPoint, ApiTemporalRange, ApiTemporalUnit } from "../../../../../../openapi";
import {
  VideoPlayerSegmentBuilderData
} from "../../../../competition/competition-builder/competition-builder-task-dialog/video-player-segment-builder/video-player-segment-builder.component";
import { TimeUtilities } from "../../../../utilities/time.utilities";

@Component({
  selector: 'app-query-description-external-video-form-field',
  templateUrl: './query-description-external-video-form-field.component.html',
  styleUrls: ['./query-description-external-video-form-field.component.scss']
})
export class QueryDescriptionExternalVideoFormFieldComponent extends QueryDescriptionExternalFormFieldComponent {

  @Input()
  startControl: UntypedFormControl
  @Input()
  endControl: UntypedFormControl
  @Input()
  unitControl

  units = [ApiTemporalUnit.FRAME_NUMBER, ApiTemporalUnit.MILLISECONDS, ApiTemporalUnit.SECONDS, ApiTemporalUnit.TIMECODE]

  timeUnitChanged($event, start: HTMLInputElement, end: HTMLInputElement){
    const type = $event.value === 'TIMECODE' ? 'text' :'number';
    if(start){
      start.type =type;
    }
    if(end){
      end.type = type;
    }
  }

  segmentBuilderData(): VideoPlayerSegmentBuilderData {
    const mediaItem = this.pathControl.value
    let start = -1;
    let end = -1;
    const unit = this.unitControl?.value ? (this.unitControl.value as ApiTemporalUnit) : ApiTemporalUnit.SECONDS;
    if (this.startControl && this.startControl.value) {
      if (this.unitControl.value === 'TIMECODE') {
        start = TimeUtilities.timeCode2Milliseconds24fps(this.startControl.value) / 1000;
      } else {
        start =
          TimeUtilities.point2Milliseconds24fps({ value: this.startControl.value, unit } as ApiTemporalPoint) / 1000;
      }
    }
    if (this.endControl && this.endControl.value) {
      if (this.unitControl.value === 'TIMECODE') {
        end = TimeUtilities.timeCode2Milliseconds24fps(this.endControl.value) / 1000;
      } else {
        end = TimeUtilities.point2Milliseconds24fps({ value: this.endControl.value, unit } as ApiTemporalPoint) / 1000;
      }
    }
    return { externalPath: this.pathControl.value, segmentStart: start, segmentEnd: end } as VideoPlayerSegmentBuilderData;
  }

  rangeChanged(range: ApiTemporalRange){
    this.startControl.setValue(range.start.value)
    this.endControl.setValue(range.end.value)
    this.unitControl.setValue(ApiTemporalUnit.SECONDS)
  }

}
