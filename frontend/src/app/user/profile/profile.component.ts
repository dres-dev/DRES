import {Component, OnInit} from '@angular/core';
import {SessionService} from '../../services/session/session.service';
import {UserDetails, UserService} from '../../../../openapi';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {FormControl, FormGroup} from '@angular/forms';

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html',
    styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {

    user: Observable<UserDetails>;

    editing = false;

    form: FormGroup = new FormGroup({
        username: new FormControl(''),
        password: new FormControl(''),
        role: new FormControl('')
    });

    constructor(
        public sessionService: SessionService,
        private router: Router,
        private route: ActivatedRoute,
        public userService: UserService
    ) {
    }

    ngOnInit(): void {
        this.user = this.userService.getApiUserInfo();
        this.user.subscribe((u) => console.log(u));
    }

    public submit() {
        if (this.form.valid) {/*
            this.authenticationService.login(this.form.controls.username.value, this.form.controls.password.value).subscribe((r) => {
                    this.snackBar.open(`Login successful!`, null, {duration: 5000});
                    this.router.navigate([this.returnUrl]);
                },
                (error) => {
                    this.snackBar.open(`Login failed: ${error.error.description}!`, null, {duration: 5000});
                });*/
        }
    }

    public reset() {
        this.toggleEdit();
        // TODO reset form components
    }

    isEditing() {
        return this.editing;
    }

    toggleEdit() {
        this.editing = !this.editing;
    }
}
