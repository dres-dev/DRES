import {CompetionBuilderService} from '../../competion-builder.service';
import {CompetitionService, RestCompetitionDescription} from '../../../../../../openapi';
import {Observable, Subscription} from 'rxjs';

export class AbstractCompetitionBuilderComponent{


    competition: RestCompetitionDescription
    competitionSub: Subscription

    constructor(public builderService: CompetionBuilderService,
                ) {

    }

    onInit(){
        this.competitionSub = this.builderService.asObservable().subscribe(c => this.competition=c)
    }

    onDestroy(){
        this.competitionSub?.unsubscribe();
    }


}
