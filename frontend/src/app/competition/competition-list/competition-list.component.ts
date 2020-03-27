import { Component, OnInit } from '@angular/core';
import {CompetitionOverview, CompetitionService} from '../../../../openapi';

@Component({
  selector: 'app-competition-list',
  templateUrl: './competition-list.component.html',
  styleUrls: ['./competition-list.component.scss']
})
export class CompetitionListComponent implements OnInit {

  /** */
  displayedColumns = ['name', 'description', 'taskCount', 'teamCount', 'actions'];
  competitions: CompetitionOverview[] = [];

  constructor(private competitionService: CompetitionService) {}

  public ngOnInit(): void {
    this.refresh()
  }


  public refresh() {
    this.competitionService.getApiCompetitionList().subscribe((results: CompetitionOverview[]) => {
      this.competitions = results;
    },
    () => {

    });
  }

}
