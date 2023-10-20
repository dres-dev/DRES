import {Component} from "@angular/core";

@Component({
    selector: 'app-error-forbidden',
    styles: [`:host {
        display: flex;
        justify-content: center;
        margin: 100px 0px;
    }

    .mat-mdc-form-field {
        width: 100%;
        min-width: 300px;
    }

    /* TODO(mdc-migration): The following rule targets internal classes of card that may no longer apply for the MDC version. */
    mat-card-title,
    mat-card-content {
        display: flex;
        justify-content: center;
    }

    .error {
        padding: 16px;
        width: 300px;
        color: white;
        background-color: darkred;
    }

    .button {
        display: flex;
        justify-content: flex-end;
    }`],
    template: `
        <mat-card appearance="outlined" class="error">
            <mat-card-title>Access denied</mat-card-title>
            <mat-card-content>
                You are not allowed to view this page. Check with the administrator if you believe this to be a mistake.
            </mat-card-content>
        </mat-card>
    `,
})
export class ForbiddenComponent {
}