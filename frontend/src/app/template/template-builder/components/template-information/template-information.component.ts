import { AfterViewInit, Component, OnDestroy, OnInit } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import { FormControl, UntypedFormControl, UntypedFormGroup, Validators } from "@angular/forms";
import { Observable, Subscription } from "rxjs";
import { ApiMediaCollection, CollectionService, TemplateService } from "../../../../../../openapi";
import { TemplateBuilderService } from "../../template-builder.service";

@Component({
  selector: "app-template-information",
  templateUrl: "./template-information.component.html",
  styleUrls: ["./template-information.component.scss"]
})
export class TemplateInformationComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy, AfterViewInit {

  form: UntypedFormGroup = new UntypedFormGroup({
    name: new UntypedFormControl(""),
    description: new UntypedFormControl(""),
    collection: new UntypedFormControl(""),
    defaultRandomLength: new FormControl<number>(20, [Validators.min(0)])
  });

  private changeSub: Subscription;

  private initOngoing = true;

  mediaCollectionSource: Observable<ApiMediaCollection[]>;

  constructor(
    private templateService: TemplateService,
    private collectionService: CollectionService,
    builder: TemplateBuilderService
  ) {
    super(builder);
  }

  ngAfterViewInit(): void {
    // builderService is not yet initialised ?!?
        this.form.get('collection').setValue(this.getMostUsedCollection(), {emitEvent: false});
    }

  ngOnInit(): void {
    this.onInit();
    this.changeSub = this.form.valueChanges.subscribe((value) => {
      let isDirty = false;
      if (value.name !== this.builderService.getTemplate().name) {
        isDirty = true;
      }
      if (value.description !== this.builderService.getTemplate().description) {
        isDirty = true;
      }
      if (value.collection && value.collection?.length > 0) {
        this.builderService.defaultCollection = value.collection;
      } else if (this.builderService.getTemplate().tasks?.length > 0) {
        /* Simply take majority of used target collection as default collection */
        const buckets = new Map<string, number>();
        this.builderService.getTemplate().tasks.forEach(t => {
          if (buckets.has(t.collectionId)) {
            buckets[t.collectionId]++;
          } else {
            buckets.set(t.collectionId, 1);
          }
        });
        const highestUsedCollection = [...buckets.entries()].sort((a, b) => b[1] - a[1])[0];
        this.builderService.defaultCollection = highestUsedCollection[0];
      }
      if (value.defaultRandomLength) {
        this.builderService.defaultSegmentLength = value.defaultRandomLength;
      }
      if (isDirty) {
        this.builderService.markDirty();
      }
    });
    this.mediaCollectionSource = this.collectionService.getApiV2CollectionList();
  }

  private getMostUsedCollection() {
    console.log("nb Tasks: ", this.builderService?.getTemplate()?.tasks?.length)
    if (this.builderService?.getTemplate()?.tasks?.length > 0) {
      const buckets = new Map<string, number>();
      this.builderService.getTemplate().tasks.forEach(t => {
        if (buckets.has(t.collectionId)) {
          buckets[t.collectionId]++;
        } else {
          buckets.set(t.collectionId, 1);
        }
      });
      const highestUsedCollection = [...buckets.entries()].sort((a, b) => b[1] - a[1])[0];
      this.builderService.defaultCollection = highestUsedCollection[0];
      console.log("Most used collection: ", highestUsedCollection[0])
      return highestUsedCollection[0];
    } else {
      console.log('No most used collection')
      return "";
    }
  }

  ngOnDestroy() {
    this.onDestroy();
    this.changeSub.unsubscribe();
  }

  onChange() {
    if (this.builderService.getTemplate()) {
      if (this.form.get("name").value !== this.builderService.getTemplate().name) {
        this.form.get("name").setValue(this.builderService.getTemplate().name, { emitEvent: !this.initOngoing });
      }
      if (this.form.get("description").value !== this.builderService.getTemplate().description) {
        this.form.get("description").setValue(this.builderService.getTemplate().description, { emitEvent: !this.initOngoing });
      }
      // We need to check the end of init here
      if (this.initOngoing) {
        this.initOngoing = false;
      }
    }
  }


}
