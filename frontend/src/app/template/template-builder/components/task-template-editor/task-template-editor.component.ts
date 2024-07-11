import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { TemplateBuilderService } from "../../template-builder.service";
import { forkJoin, Observable, Subscription } from "rxjs";
import {
  ApiHintOption, ApiHintType,
  ApiMediaCollection, ApiMediaItem, ApiTarget, ApiTargetOption, ApiTargetType,
  ApiTaskGroup,
  ApiTaskTemplate,
  ApiTaskType, ApiTemporalPoint, ApiTemporalRange,
  ApiTemporalUnit,
  CollectionService, MediaService
} from "../../../../../../openapi";
import { UntypedFormControl, UntypedFormGroup } from "@angular/forms";
import {
  CompetitionFormBuilder
} from "../../../../competition/competition-builder/competition-builder-task-dialog/competition-form.builder";
import {
  VideoPlayerSegmentBuilderData
} from "../../../../competition/competition-builder/competition-builder-task-dialog/video-player-segment-builder/video-player-segment-builder.component";
import { AppConfig } from "../../../../app.config";
import { MatDialog, MatDialogConfig } from "@angular/material/dialog";
import { filter, first, map } from "rxjs/operators";
import { TimeUtilities } from "../../../../utilities/time.utilities";
import {
  AdvancedBuilderDialogComponent,
  AdvancedBuilderDialogData
} from "../../../../competition/competition-builder/competition-builder-task-dialog/advanced-builder-dialog/advanced-builder-dialog.component";
import { BatchAddTargetDialogComponent, BatchAddTargetDialogData } from "../batch-add-target-dialog/batch-add-target-dialog.component";

@Component({
  selector: 'app-task-template-editor',
  templateUrl: './task-template-editor.component.html',
  styleUrls: ['./task-template-editor.component.scss']
})
export class TaskTemplateEditorComponent  implements OnInit, OnDestroy {

  public task: ApiTaskTemplate;

  public taskType: ApiTaskType;
  public taskGroup: ApiTaskGroup;

  form: UntypedFormGroup;

  units = [ApiTemporalUnit.FRAME_NUMBER, ApiTemporalUnit.SECONDS, ApiTemporalUnit.MILLISECONDS, ApiTemporalUnit.TIMECODE]

  mediaCollectionSource: Observable<ApiMediaCollection[]>;

  formBuilder: CompetitionFormBuilder;

  @ViewChild('videoPlayer', {static: false}) video: ElementRef;

  viewLayout = 'list';

  showVideo = false;
  videoSegmentData: VideoPlayerSegmentBuilderData;

  externalImagePreviewActive = false;
  externalImagePreviewUrl = ''
  externalVideoPreviewActive = false;
  externalVideoData: VideoPlayerSegmentBuilderData

  private imagePreviewMap = new Set<number>();
  private taskSub: Subscription;

  taskGroupOptions: ApiTaskGroup[]

  constructor(private builderService: TemplateBuilderService,
              public collectionService: CollectionService,
              public config: AppConfig,
              private dialog: MatDialog) {}

  ngOnInit(): void {
    this.taskSub = this.builderService.selectedTaskTemplateAsObservable().subscribe((t)=>{
      if(t){
        this.task = t;
        this.taskGroup = this.builderService.selectedTaskGroup;
        this.taskType = this.builderService.selectedTaskType;
        this.init();
      }else{
        this.task = null;
        this.taskGroup = null;
        this.taskType = null;
      }
    })




  }

  ngOnDestroy() {
    this.taskSub.unsubscribe();
    this.taskSub = null;
  }

  public init(){
    this.formBuilder = new CompetitionFormBuilder(this.taskGroup, this.taskType, this.collectionService, this.builderService, this.task);
    this.form = this.formBuilder.form;
    this.form.valueChanges.subscribe(newValue => {
      this.formBuilder.storeFormData();
      this.builderService.markDirty();
    });
    this.mediaCollectionSource = this.collectionService.getApiV2CollectionList();
    this.taskGroupOptions = this.builderService.findGroupsByType(this.taskType)
    /* Close open video preview */
    this.showVideo = false;
  }

  public isFormValid(){
    if(this.builderService.hasTouchedTasks()){
      return this?.form?.valid || true;
    }else{
      return true;
    }
  }

  public hasCollectionSet(){
    if(this.form){
      if(this.form.get('mediaCollection')?.value){
        return true
      }
    }
    return false
  }

  public fetchData(){
    return this.formBuilder.fetchFormData();
  }

  private static randInt(min: number, max: number): number {
    min = Math.floor(min);
    max = Math.ceil(max);
    return Math.round(Math.random() * (max - min + 1) + min);
  }

  uploaded = (taskData: string) => {
    const task = JSON.parse(taskData) as ApiTaskTemplate;
    this.formBuilder = new CompetitionFormBuilder(this.taskGroup, this.taskType, this.collectionService, this.builderService, task);
    this.form = this.formBuilder.form;
  };

  /**
   * Handler for (+) button for query target form component.
   */
  public addQueryTarget(targetType: ApiTargetOption) {
    if(targetType){
      this.formBuilder.addTargetForm(targetType);
    }else{
      this.formBuilder.addTargetForm(this.taskType.targetOption)
    }
  }

  /**
   * Handler for (-) button for query target form component.
   *
   * @param index The index of the query target to remove.
   */
  public removeQueryTarget(index: number) {
    this.formBuilder.removeTargetForm(index);
  }

  /**
   * Handler for (+) button for query hint form component.
   */
  public addQueryComponent(componentType: ApiHintOption, previous: number = null) {
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
        this.formBuilder.addComponentForm(ApiHintType.IMAGE, previous, true); // FIXME not entirely supported
        break;
      case 'EXTERNAL_VIDEO':
        this.formBuilder.addComponentForm(ApiHintType.VIDEO, previous, true); // FIXME not entirely supported
        break;
    }
  }

  /**
   * Handler for (-) button for query hint form components.
   *
   * @param index The index of the query component to remove.
   */
  public removeQueryComponent(index: number) {
    this.formBuilder.removeComponentForm(index);
  }

  /**
   * Converts a MediaItem to its display value for the autocomplete field.
   *
   * @param value MediaItem to convert
   */
  public mediaItemToDisplay(value: ApiMediaItem) {
    if (value) {
      return `${value.name} (${value.type})`;
    } else {
      return '';
    }
  }

  public sortMediaItemByName = (itemA: ApiMediaItem, itemB: ApiMediaItem) => {
    return itemA.name.localeCompare(itemB.name);
  }


  /**
   * The form data as json
   */
  asJson(): string {
    return JSON.stringify(this.formBuilder.fetchFormData());
  }

  fileProvider = () => (this.formBuilder.fetchFormData()?.name ? this.formBuilder.fetchFormData().name : 'task-download.json');

  downloadProvider = () => this.asJson();

  /**
   * Picks a ranomd {@link MediaItem} from the list.
   *
   * @param collectionId The ID of the collection to pick a {@link MediaItem} from.
   * @param target The target {@link FormControl} to apply the value to.
   */
  public pickRandomMediaItem(collectionId: string, target: UntypedFormControl) {
    this.collectionService
      .getApiV2CollectionByCollectionIdRandom(collectionId)
      .pipe(first())
      .subscribe((value) => {
        target.setValue(value);
      });
  }

  /**
   * Picks a random segment within the given {@link MediaItem} .
   *
   * @param item The {@link VideoItem} to pick the segment for.
   * @param startControl The target {@link FormControl} to apply the value to.
   * @param endControl The target {@link FormControl} to apply the value to.
   * @param unitControl The target {@link FormControl} to apply the value to.
   */
  public pickRandomSegment(item: ApiMediaItem, startControl: UntypedFormControl, endControl: UntypedFormControl, unitControl: UntypedFormControl) {
    const start = TaskTemplateEditorComponent.randInt(1, item.durationMs / 1000 / 2); // always in first half
    let end = 1;
    if(this.builderService.defaultSegmentLength === 0){
      console.log("Using random length for random segment")
      do {
        end = start + TaskTemplateEditorComponent.randInt(5, item.durationMs / 1000); // Arbitrary 5 seconds minimal length
      } while (end > item.durationMs / 1000);
    }else{
      console.log("Using default length for random segment (start, defaultLength)", start, this.builderService.defaultSegmentLength)
      end = start + this.builderService.defaultSegmentLength;
      if(end > item.durationMs / 1000){
        end = (item.durationMs / 1000) - start;
      }
    }

    startControl.setValue(start);
    endControl.setValue(end);
    unitControl.setValue('SECONDS');
  }

  toggleExternalVideoPreview(path: string, startControl?: UntypedFormControl, endControl?: UntypedFormControl, unitControl?: UntypedFormControl) {
    let start = -1;
    let end = -1;
    const unit = unitControl?.value ? (unitControl.value as ApiTemporalUnit) : ApiTemporalUnit.SECONDS;
    if (startControl && startControl.value) {
      if (unitControl.value === 'TIMECODE') {
        start = TimeUtilities.timeCode2Milliseconds24fps(startControl.value) / 1000;
      } else {
        start =
          TimeUtilities.point2Milliseconds24fps({ value: startControl.value, unit } as ApiTemporalPoint) / 1000;
      }
    }
    if (endControl && endControl.value) {
      if (unitControl.value === 'TIMECODE') {
        end = TimeUtilities.timeCode2Milliseconds24fps(endControl.value) / 1000;
      } else {
        end = TimeUtilities.point2Milliseconds24fps({ value: endControl.value, unit } as ApiTemporalPoint) / 1000;
      }
    }

    console.log('Start=' + start + ', End=' + end);
    this.externalVideoData = { externalPath: path, segmentStart: start, segmentEnd: end } as VideoPlayerSegmentBuilderData;
    this.externalVideoPreviewActive = !this.externalVideoPreviewActive;
  }

  externalPreviewActive():boolean{
    return this.externalImagePreviewActive || this.externalVideoPreviewActive
  }

  toggleExternalImagePreview(path: string){
    if(this.externalImagePreviewActive){
      this.externalImagePreviewActive = false
      this.externalImagePreviewUrl = ''
    }else{
      this.externalImagePreviewActive = true
      this.externalImagePreviewUrl = this.config.resolveExternalUrl(path)
    }
  }

  toggleVideoPlayer(mediaItem: ApiMediaItem, startControl?: UntypedFormControl, endControl?: UntypedFormControl, unitControl?: UntypedFormControl) {
    /* Add to toggleVideoPlayer button if
        [disabled]="!target.get('mediaItem').value && !target.get('segment_start').value && !target.get('segment_end').value"
         */
    /*
        convert segmentStart / end based on unit to seconds
        pass everything to dialog. let dialog handle and take result as temporal range
         */
    let start = -1;
    let end = -1;
    const unit = unitControl?.value ? (unitControl.value as ApiTemporalUnit) : ApiTemporalUnit.SECONDS;
    if (startControl && startControl.value) {
      if (unitControl.value === 'TIMECODE') {
        start = TimeUtilities.timeCode2Milliseconds(startControl.value, mediaItem.fps) / 1000;
      } else {
        start =
          TimeUtilities.point2Milliseconds({ value: startControl.value, unit } as ApiTemporalPoint, mediaItem.fps) / 1000;
      }
      // start = Number.parseInt(startControl.value, 10);
    }
    if (endControl && endControl.value) {
      if (unitControl.value === 'TIMECODE') {
        end = TimeUtilities.timeCode2Milliseconds(endControl.value, mediaItem.fps) / 1000;
      } else {
        end = TimeUtilities.point2Milliseconds({ value: endControl.value, unit } as ApiTemporalPoint, mediaItem.fps) / 1000;
      }
    }

    console.log('Start=' + start + ', End=' + end);
    // const config = {
    //     width: '800px', data: {mediaItem, segmentStart: start, segmentEnd: end}
    // } as MatDialogConfig<VideoPlayerSegmentBuilderData>;
    // const dialogRef = this.dialog.open(VideoPlayerSegmentBuilderDialogComponent, config);
    /*dialogRef.afterClosed().pipe(
            filter(r => r != null))
            .subscribe((r: TemporalRange) => {
                console.log(`Finished: ${r}`);
                startControl.setValue(r.start.value);
                endControl.setValue(r.end.value);
                unitControl.setValue(TemporalPoint.UnitEnum.SECONDS);
            });*/
    this.videoSegmentData = { mediaItem, segmentStart: start, segmentEnd: end } as VideoPlayerSegmentBuilderData;
    this.showVideo = !this.showVideo;
  }

  onRangeChange(range: ApiTemporalRange, startControl?: UntypedFormControl, endControl?: UntypedFormControl, unitControl?: UntypedFormControl) {
    startControl?.setValue(range.start.value);
    endControl?.setValue(range.end.value);
    unitControl?.setValue(ApiTemporalUnit.SECONDS);
  }

  isImageMediaItem(mi: ApiMediaItem): boolean {
    if (mi) {
      return mi.type === 'IMAGE';
    } else {
      return false;
    }
  }

  /**
   * Check whether the given index is currently listed as active preview
   *
   * @param index
   */
  isPreviewActive(index: number): boolean {
    return this.imagePreviewMap.has(index);
  }

  togglePreview(index: number) {
    if (this.imagePreviewMap.has(index)) {
      this.imagePreviewMap.delete(index);
    } else {
      this.imagePreviewMap.add(index);
    }
  }


  batchAddTargets() {
    const config = {
      width: '400px'
    } as MatDialogConfig;
    const dialogRef = this.dialog.open(BatchAddTargetDialogComponent, config);
    dialogRef
      .afterClosed()
      .pipe(filter((r) => r != null))
      .subscribe((r: Array<string>) => {
        let targets : ApiTarget[]
        switch(this.taskType.targetOption){
          case "SINGLE_MEDIA_ITEM":
          case "SINGLE_MEDIA_SEGMENT":
            this.collectionService.postApiV2CollectionByCollectionIdResolve(this.form.get('mediaCollection').value, r)
              .subscribe(items => {
                items.forEach(it => {
                  const type = this.taskType.targetOption === "SINGLE_MEDIA_ITEM" ? ApiTargetType.MEDIA_ITEM : ApiTargetType.MEDIA_ITEM_TEMPORAL_RANGE;
                  this.formBuilder.addTargetForm(this.taskType.targetOption, {type: type, target: it.mediaItemId} as ApiTarget, true, it)
                })
              })
            break;
          case "JUDGEMENT":
          case "VOTE":
            console.warn("Cannot batch-add targets for target option JUDGEMENT or VOTE.")
            break;
          case "TEXT":
            const targets = r.map(it => {
              return {target: it, type: ApiTargetType.TEXT} as ApiTarget
            });
            targets.forEach(target => {
              this.formBuilder.addTargetForm(this.taskType.targetOption, target)
            })
            break;
        }
      });
  }

  timeUnitChanged($event, startElementRef: HTMLInputElement, endElementRef: HTMLInputElement) {
    console.log($event);
    const type = $event.value === 'TIMECODE' ? 'text' : 'number';
    console.log('New type: ' + type);
    if (startElementRef) {
      startElementRef.type = type;
    }
    if (endElementRef) {
      endElementRef.type = type;
    }
  }

  /**
   * Handler for 'close' button.
   */
  private pathForItem(item: ApiMediaItem): string {
    // units = ['FRAME_NUMBER', 'SECONDS', 'MILLISECONDS', 'TIMECODE'];
    let timeSuffix = '';
    switch (this.form.get('time_unit').value) {
      case 'FRAME_NUMBER':
        const start = Math.round(this.form.get('start').value / item.fps);
        const end = Math.round(this.form.get('end').value / item.fps);
        timeSuffix = `#t=${start},${end}`;
        break;
      case 'SECONDS':
        timeSuffix = `#t=${this.form.get('start').value},${this.form.get('end').value}`;
        break;
      case 'MILLISECONDS':
        timeSuffix = `#t=${Math.round(this.form.get('start').value / 1000)},${Math.round(this.form.get('end').value / 1000)}`;
        break;
      case 'TIMECODE':
        console.log('Not yet supported'); // TODO make it!
        break;
      default:
        console.error(`The time unit ${this.form.get('time_unit').value} is not supported`);
    }
    return '';
  }

  public renderTextTargetTooltip() {
    return `The textual task target.
        Regex are allowed and have to be enclosed with single backslashes (\\).
        Java Regex matching is used.`;
  }

  close() {
    this.builderService.selectTaskTemplate(null);
  }
}
