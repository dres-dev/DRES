import {Directive, HostListener} from '@angular/core';
import {NavigationService} from './navigation.service';

@Directive({
  selector: '[appForwardButton]'
})
export class ForwardButtonDirective {

  constructor(private navigation: NavigationService) {
  }

  @HostListener('click')
  onClick(): void {
    this.navigation.forward();
  }

}
