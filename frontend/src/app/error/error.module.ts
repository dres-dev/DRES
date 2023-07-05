import {NgModule} from "@angular/core";
import {ForbiddenComponent} from "./forbidden.component";
import {NotFoundComponent} from "./not-found.component";
import {MatCardModule} from "@angular/material/card";

@NgModule({
    declarations: [
        ForbiddenComponent,
        NotFoundComponent
    ],
    imports: [
        MatCardModule
    ],
    exports: []
})
export class ErrorModule {
}
