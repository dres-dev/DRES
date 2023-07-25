import { AfterViewInit, Component, OnDestroy, OnInit } from "@angular/core";
import { AbstractTemplateBuilderComponent } from "../abstract-template-builder.component";
import {FormControl, FormGroup, UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import { Observable, Subscription } from "rxjs";
import { ApiMediaCollection, CollectionService, TemplateService } from "../../../../../../openapi";
import { TemplateBuilderService } from "../../template-builder.service";
import { ActivatedRoute } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";

@Component({
  selector: "app-template-information",
  templateUrl: "./template-information.component.html",
  styleUrls: ["./template-information.component.scss"]
})
export class TemplateInformationComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy, AfterViewInit {

  form: FormGroup = new UntypedFormGroup({
    name: new FormControl("", [Validators.required]),
    description: new FormControl(""),
    collection: new FormControl(""),
    defaultRandomLength: new FormControl<number>(0, [Validators.min(0)]),
    created: new FormControl('', [Validators.required]),
    modified: new FormControl('', [Validators.required])
  });

  private changeSub: Subscription;

  private initOngoing = true;

  mediaCollectionSource: Observable<ApiMediaCollection[]>;

  constructor(
    templateService: TemplateService,
    private collectionService: CollectionService,
    builder: TemplateBuilderService,
    route: ActivatedRoute,
    snackBar: MatSnackBar,
  ) {
    super(builder, route, templateService, snackBar);
  }

  ngAfterViewInit(): void {
    // builderService is not yet initialised ?!?
  }

  ngOnInit(): void {
    this.onInit();
    this.routeSub = this.route.params.subscribe( (p) => {
      this.templateService.getApiV2TemplateByTemplateId(p.templateId).subscribe((t) => {
          /* initialise from route */
          this.builderService.initialise(t);
        },
        (r) => {
          this.snackBar.open(`Error: ${r?.error?.description}`, null,{duration: 5000});
        });
    });
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
        console.log("Highest Used Collections", highestUsedCollection)
        this.builderService.defaultCollection = highestUsedCollection[0];
      }
      if (value.defaultRandomLength) {
        this.builderService.defaultSegmentLength = value.defaultRandomLength;
      }
      if (isDirty) {
        this.builderService.getTemplate().name = value.name;
        this.builderService.getTemplate().description = value.description;
        this.builderService.markDirty();
      }
    });
    this.mediaCollectionSource = this.collectionService.getApiV2CollectionList();
  }

  private getMostUsedCollection() {
    console.log("template: ", this.builderService.getTemplate())
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
    if (this.builderService.getTemplate() && this.getTemplateId() === this.builderService.getTemplate().id) {
      console.log("Change", this.builderService.getTemplate(), this.form, this.initOngoing)
      if (this.form.get("name").value !== this.builderService.getTemplate().name) {
        this.form.get("name").setValue(this.builderService.getTemplate().name, { emitEvent: !this.initOngoing });
      }
      if (this.form.get("description").value !== this.builderService.getTemplate().description) {
        this.form.get("description").setValue(this.builderService.getTemplate().description, { emitEvent: !this.initOngoing });
      }
      if (this.form.get("created").value !== this.builderService.getTemplate().created) {
        this.form.get("created").setValue(this.builderService.getTemplate().created, { emitEvent: !this.initOngoing });
      }
      if (this.form.get("modified").value !== this.builderService.getTemplate().modified) {
        this.form.get("modified").setValue(this.builderService.getTemplate().modified, { emitEvent: !this.initOngoing });
      }
      // We need to check the end of init here
      if (this.initOngoing) {
        const defaultCollection = this.getMostUsedCollection();
        this.form.get("collection").setValue(defaultCollection, {emitEvent: false});
        this.builderService.defaultCollection = defaultCollection;
        this.initOngoing = false;
      }
    }
  }
}
