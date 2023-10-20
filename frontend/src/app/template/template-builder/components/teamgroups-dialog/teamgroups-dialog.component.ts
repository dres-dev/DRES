import { Component, Inject } from "@angular/core";
import { FormControl, FormGroup, UntypedFormGroup, Validators } from "@angular/forms";
import { ApiTeam, ApiTeamAggregatorType, ApiTeamGroup } from "../../../../../../openapi";
import { Observable } from "rxjs";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TemplateBuilderService } from "../../template-builder.service";
import { AppConfig } from "../../../../app.config";
import { map, shareReplay } from "rxjs/operators";
import { MatAutocompleteSelectedEvent } from "@angular/material/autocomplete";

@Component({
  selector: 'app-teamgroups-dialog',
  templateUrl: './teamgroups-dialog.component.html',
  styleUrls: ['./teamgroups-dialog.component.scss']
})
export class TeamgroupsDialogComponent {

  form: UntypedFormGroup;
  availableTeams: Observable<ApiTeam[]>;

  aggregatorTypes = Object.values(ApiTeamAggregatorType)

  constructor(
    private dialogRef: MatDialogRef<TeamgroupsDialogComponent>,
    private builderService: TemplateBuilderService,
    private config: AppConfig,
    @Inject(MAT_DIALOG_DATA) private teamgroup?: ApiTeamGroup
  ) {

    this.form = new UntypedFormGroup({
      id: new FormControl(teamgroup?.id),
      name: new FormControl(teamgroup?.name, [Validators.required, Validators.minLength(3)]),
      aggregation: new FormControl(teamgroup?.aggregation, [Validators.required, Validators.minLength(3)]),
      teams: new FormControl(teamgroup?.teams || [], [Validators.minLength(1)]),
      teamInput: new FormControl('')
    });

    this.availableTeams = this.builderService.templateAsObservable().pipe(map(t => {
      if(t){
        return t.teams
      }else{
        return [];
      }
    }), shareReplay(1));

  }

  public addTeam(event: MatAutocompleteSelectedEvent){
    this.form.get('teams').value.push(event.option.value);
    this.form.get('teamInput').setValue(null);
  }

  public removeTeam(team: ApiTeam){
    const idx = this.form.get('teams').value.indexOf(team);
    if(idx >= 0){
      this.form.get('teams').value.splice(idx, 1);
    }
  }

  public teamForId(id: string): Observable<ApiTeam>{
    return this.availableTeams.pipe(map((teams) => teams.find((t) => t.teamId === id)));
  }

  private fetchData(){
    return {
      id: this.form.get('id')?.value,
      name: this.form.get('name').value,
      aggregation: this.form.get('aggregation').value,
      teams: this.form.get('teams').value
    } as ApiTeamGroup
  }

  public save(){
    if(this.form.valid){
      this.dialogRef.close(this.fetchData())
    }else{
      console.log("TeamGroup Dialog", this.fetchData())
      console.log(this.form.get('teams').errors)
    }
  }

  public close(){
    this.dialogRef.close(null);
  }

}
