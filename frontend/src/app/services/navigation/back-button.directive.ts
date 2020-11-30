import {Directive, HostListener} from '@angular/core';
import {NavigationService} from './navigation.service';

@Directive({
    selector: '[appBackButton]'
})
export class BackButtonDirective {

    constructor(private navigation: NavigationService) {
    }

    @HostListener('click')
    onClick(): void {
        this.navigation.back();
    }

}
