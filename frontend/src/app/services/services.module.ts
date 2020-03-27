import {ApiModule, Configuration} from '../../../openapi';
import {SessionService} from './session/session.service';
import {NgModule} from '@angular/core';

@NgModule({
    imports: [ ApiModule.forRoot(() => {
        return new Configuration({
            basePath: `http://localhost:8080`,
            withCredentials: true
        });
    })],
    exports:      [ ApiModule ],
    declarations: [ ],
    providers:    [ SessionService ]
})
export class ServicesModule { }
