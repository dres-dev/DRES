import {Component, OnDestroy, OnInit} from '@angular/core';
import {AbstractTemplateBuilderComponent} from '../abstract-template-builder.component';
import {FormControl, FormGroup} from '@angular/forms';
import { Observable, Subscription } from "rxjs";
import { ApiMediaCollection, CollectionService, TemplateService } from "../../../../../../openapi";
import {TemplateBuilderService} from '../../template-builder.service';

@Component({
  selector: 'app-template-information',
  templateUrl: './template-information.component.html',
  styleUrls: ['./template-information.component.scss']
})
export class TemplateInformationComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {

  form: FormGroup = new FormGroup({
    name: new FormControl(''),
    description: new FormControl(''),
    collection: new FormControl('')
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

  ngOnInit(): void {
    this.onInit();
    this.changeSub = this.form.valueChanges.subscribe((value) => {
      let isDirty = false;
      if(value.name !== this.builderService.getTemplate().name){
        isDirty = true;
      }
      if(value.description !== this.builderService.getTemplate().description){
        isDirty = true;
      }
      if(value.collection && value.collection?.length > 0){
        this.builderService.defaultCollection = value.collection;
      }
      if(isDirty){
        this.builderService.markDirty();
      }
    });
    this.mediaCollectionSource = this.collectionService.getApiV2CollectionList();
  }

  ngOnDestroy() {
    this.onDestroy();
    this.changeSub.unsubscribe();
  }

  onChange() {
    if(this.builderService.getTemplate()){
      if(this.form.get('name').value !== this.builderService.getTemplate().name){
        this.form.get('name').setValue(this.builderService.getTemplate().name, {emitEvent: !this.initOngoing});
      }
      if(this.form.get('description').value !== this.builderService.getTemplate().description){
        this.form.get('description').setValue(this.builderService.getTemplate().description, {emitEvent: !this.initOngoing});
      }
      // We need to check the end of init here
      if(this.initOngoing){
        this.initOngoing = false;
      }
    }
  }


}
