import { Directive, HostListener } from '@angular/core';
import { NavigationService } from './navigation.service';

@Directive({
    selector: '[appForwardButton]',
    standalone: false
})
export class ForwardButtonDirective {
  constructor(private navigation: NavigationService) {}

  @HostListener('click')
  onClick(): void {
    this.navigation.forward();
  }
}
