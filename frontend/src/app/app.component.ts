import {Component, Inject} from '@angular/core';
import {SessionService} from './services/session/session.service';
import {Router} from '@angular/router';
import {AuthenticationService} from './services/session/authentication.sevice';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'dres-frontend';


  constructor(private authenticationService: AuthenticationService,
              public sessionService: SessionService,
              private router: Router) {}

  public logout() {
    this.authenticationService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}
