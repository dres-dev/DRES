import {NgModule} from "@angular/core";
import {ForbiddenComponent} from "./forbidden.component";
import {NotFoundComponent} from "./not-found.component";
import {MatCardModule} from "@angular/material/card";
import { ErrorDialogComponent } from './error-dialog/error-dialog.component';
import { MatDialogModule } from "@angular/material/dialog";
import { SharedModule } from "../shared/shared.module";
import { ErrorDialogService } from "./error-dialog.service";

@NgModule({
    declarations: [
        ForbiddenComponent,
        NotFoundComponent,
        ErrorDialogComponent
    ],
  imports: [
    MatCardModule,
    MatDialogModule,
    SharedModule
  ],
    exports: [],
  providers: [ErrorDialogService]
})
export class ErrorModule {
}
