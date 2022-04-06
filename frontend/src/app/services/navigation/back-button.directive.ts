import {Directive, HostListener, Input} from '@angular/core';
import {NavigationService} from './navigation.service';

@Directive({
    selector: '[appBackButton]'
})
export class BackButtonDirective {

    @Input() appBackButton = false;

    constructor(private navigation: NavigationService) {
    }

    @HostListener('click')
    onClick(): void {
        this.navigation.back(this.appBackButton);
    }

}
