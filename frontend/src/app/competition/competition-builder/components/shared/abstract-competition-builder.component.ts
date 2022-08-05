import {CompetitionBuilderService} from '../../competition-builder.service';
import {RestCompetitionDescription} from '../../../../../../openapi';
import {Subscription} from 'rxjs';

export abstract class AbstractCompetitionBuilderComponent {


    competition: RestCompetitionDescription;
    competitionSub: Subscription;

    protected constructor(public builderService: CompetitionBuilderService,
    ) {

    }

    onInit() {
        this.competitionSub = this.builderService.asObservable().subscribe(c => {
            this.competition = c;
            this.onChange(c);
        });
    }

    onDestroy() {
        this.competitionSub?.unsubscribe();
    }

    update() {
        this.builderService.update(this.competition);
    }

    onChange(competition: RestCompetitionDescription){

    }


}
