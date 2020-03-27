import {AfterViewInit, Component, OnInit} from '@angular/core';
import {CompetitionOverview, CompetitionService} from '../../../../openapi';

@Component({
  selector: 'app-competition-list',
  templateUrl: './competition-list.component.html',
  styleUrls: ['./competition-list.component.scss']
})
export class CompetitionListComponent implements AfterViewInit {

  /** */
  displayedColumns = ['actions', 'id', 'name', 'description', 'taskCount', 'teamCount'];
  competitions: CompetitionOverview[] = [];

  constructor(private competitionService: CompetitionService) {}

  public refresh() {
    this.competitionService.getApiCompetitionList().subscribe((results: CompetitionOverview[]) => {
      this.competitions = results;
    },
    () => {

    });
  }

  ngAfterViewInit(): void {
    this.refresh()
  }

}
