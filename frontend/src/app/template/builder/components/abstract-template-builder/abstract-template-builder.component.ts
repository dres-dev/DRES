import { Component, OnInit } from '@angular/core';
import {ApiEvaluationTemplate} from '../../../../../../openapi';
import {Subscription} from 'rxjs';
import {TemplateBuilderService} from '../../template-builder.service';

export abstract class AbstractTemplateBuilderComponent {

  subscription: Subscription;
  protected constructor(protected builderService: TemplateBuilderService) { }

  onInit(): void {
    this.subscription = this.builderService.templateAsObservable().subscribe((t) => {
      this.onChange();
    })
  }

  onDestroy(){
    this.subscription?.unsubscribe();
  }

  abstract onChange();

}
