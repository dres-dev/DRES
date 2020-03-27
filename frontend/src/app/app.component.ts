import {Component, Inject} from '@angular/core';
import {SessionService} from './services/session/session.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'dres-frontend';


  constructor(@Inject(SessionService) public sessionService: SessionService, private router: Router) {

  }

  public logout() {
    this.sessionService.logout().subscribe(() => {
      this.router.navigateByUrl('/login');
    });
  }
}
