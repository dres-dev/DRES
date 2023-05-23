import { Component, Input } from "@angular/core";
import {
  QueryDescriptionMediaItemFormFieldComponent
} from "../query-description-media-item-form-field/query-description-media-item-form-field.component";
import { UntypedFormControl } from "@angular/forms";
import { ApiTemporalPoint, ApiTemporalRange, ApiTemporalUnit } from "../../../../../../openapi";
import {
  VideoPlayerSegmentBuilderData
} from "../../../../competition/competition-builder/competition-builder-task-dialog/video-player-segment-builder/video-player-segment-builder.component";
import { TimeUtilities } from "../../../../utilities/time.utilities";

@Component({
  selector: 'app-query-description-media-item-video-form-field',
  templateUrl: './query-description-media-item-video-form-field.component.html',
  styleUrls: ['./query-description-media-item-video-form-field.component.scss']
})
export class QueryDescriptionMediaItemVideoFormFieldComponent extends QueryDescriptionMediaItemFormFieldComponent{

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
    const mediaItem = this.itemControl.value
    let start = -1;
    let end = -1;
    const unit = this.unitControl?.value ? (this.unitControl.value as ApiTemporalUnit) : ApiTemporalUnit.SECONDS;
    if (this.startControl && this.startControl.value) {
      if (this.unitControl.value === 'TIMECODE') {
        start = TimeUtilities.timeCode2Milliseconds(this.startControl.value, mediaItem.fps) / 1000;
      } else {
        start =
          TimeUtilities.point2Milliseconds({ value: this.startControl.value, unit } as ApiTemporalPoint, mediaItem.fps) / 1000;
      }
    }
    if (this.endControl && this.endControl.value) {
      if (this.unitControl.value === 'TIMECODE') {
        end = TimeUtilities.timeCode2Milliseconds(this.endControl.value, mediaItem.fps) / 1000;
      } else {
        end = TimeUtilities.point2Milliseconds({ value: this.endControl.value, unit } as ApiTemporalPoint, mediaItem.fps) / 1000;
      }
    }
    return { mediaItem, segmentStart: start, segmentEnd: end } as VideoPlayerSegmentBuilderData;
  }

  rangeChanged(range: ApiTemporalRange){
    this.startControl.setValue(range.start.value)
    this.endControl.setValue(range.end.value)
    this.unitControl.setValue(ApiTemporalUnit.SECONDS)
  }

}
