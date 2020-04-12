import {Component} from '@angular/core';
import {SessionService} from './services/session/session.service';
import {Router} from '@angular/router';
import {AuthenticationService} from './services/session/authentication.sevice';
import {UserDetails} from '../../openapi';
import {MatSnackBar} from '@angular/material/snack-bar';
import RoleEnum = UserDetails.RoleEnum;

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent {
    title = 'dres-frontend';


    constructor(public authenticationService: AuthenticationService,
                public sessionService: SessionService,
                private router: Router,
                private snackBar: MatSnackBar) {
    }

    public logout() {
        this.authenticationService.logout().subscribe(() => {
            this.snackBar.open(`Logout Successful!`, null, {duration: 5000});
            console.log(`Logout happened. Username: {}`, this.sessionService.getUsername());
            this.router.navigate(['/']);
        });
    }

    profile() {
        this.router.navigate(['/user']);
    }

    isAdmin() {
        return this.sessionService.getRole() === RoleEnum.ADMIN;
    }
}
