import {Component, OnDestroy, OnInit} from '@angular/core';
import {AbstractTemplateBuilderComponent} from '../abstract-template-builder/abstract-template-builder.component';
import {ActivatedRoute} from '@angular/router';
import {TemplateService} from '../../../../../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import {TemplateBuilderService} from '../../template-builder.service';
import {Subscription} from 'rxjs';
import {FormControl, FormGroup} from '@angular/forms';

@Component({
    selector: 'app-information',
    templateUrl: './information.component.html',
    styleUrls: ['./information.component.scss']
})
export class InformationComponent extends AbstractTemplateBuilderComponent implements OnInit, OnDestroy {

    form: FormGroup = new FormGroup({name: new FormControl(''), description: new FormControl('')});

    private changeSub: Subscription;

    constructor(
        private templateService: TemplateService,
        builder: TemplateBuilderService
    ) {
        super(builder);
    }

    ngOnInit(): void {
        this.onInit();

        this.changeSub = this.form.valueChanges.subscribe(() => {
           this.builderService.markDirty();
        });
    }

    ngOnDestroy() {
        this.onDestroy();
        this.changeSub.unsubscribe();
    }

    onChange() {
        if(this.builderService.getTemplate()){
            this.form.get('name').setValue(this.builderService.getTemplate().name);
            this.form.get('description').setValue(this.builderService.getTemplate().description);
        }
    }
}
