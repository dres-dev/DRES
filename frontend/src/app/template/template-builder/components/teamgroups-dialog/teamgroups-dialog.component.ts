import { Component, Inject } from '@angular/core';
import { FormControl, FormGroup, UntypedFormGroup, Validators } from '@angular/forms';
import { ApiTeam, ApiTeamAggregatorType, ApiTeamGroup } from '../../../../../../openapi';
import { combineLatest, Observable, startWith } from 'rxjs';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TemplateBuilderService } from '../../template-builder.service';
import { AppConfig } from '../../../../app.config';
import { map, shareReplay } from 'rxjs/operators';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';

@Component({
  selector: 'app-teamgroups-dialog',
  templateUrl: './teamgroups-dialog.component.html',
  styleUrls: ['./teamgroups-dialog.component.scss'],
  standalone: false,
})
export class TeamgroupsDialogComponent {
  form: UntypedFormGroup;
  availableTeams: Observable<ApiTeam[]>;

  aggregatorTypes = Object.values(ApiTeamAggregatorType);

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
      /* Copy, so that the template's team group is only changed when the dialog is saved. */
      teams: new FormControl([...(teamgroup?.teams ?? [])], [Validators.minLength(1)]),
      teamInput: new FormControl(''),
    });

    this.availableTeams = combineLatest([
      this.builderService.templateAsObservable(),
      this.form.get('teamInput').valueChanges.pipe(startWith('')),
      this.form.get('teams').valueChanges.pipe(startWith(this.form.get('teams').value)),
    ]).pipe(
      map(([template, input, selected]) => {
        const filterValue = (typeof input === 'string' ? input : '').toLowerCase();
        return (template?.teams ?? [])
          .filter((team) => !(selected as ApiTeam[]).some((member) => this.sameTeam(member, team)))
          .filter((team) => !filterValue || team.name?.toLowerCase().includes(filterValue));
      }),
      shareReplay(1)
    );
  }

  public addTeam(event: MatAutocompleteSelectedEvent, input?: HTMLInputElement) {
    const team = event.option.value as ApiTeam;
    const teams = this.form.get('teams').value as ApiTeam[];
    if (!teams.some((member) => this.sameTeam(member, team))) {
      this.form.get('teams').setValue([...teams, team]);
    }
    this.form.get('teamInput').setValue('');
    if (input) {
      input.value = '';
    }
  }

  public removeTeam(team: ApiTeam) {
    const teams = this.form.get('teams').value as ApiTeam[];
    this.form.get('teams').setValue(teams.filter((member) => !this.sameTeam(member, team)));
  }

  /** Teams are compared by ID; unsaved teams are compared by name. */
  private sameTeam(a: ApiTeam, b: ApiTeam): boolean {
    return a.id || b.id ? a.id === b.id : a.name === b.name;
  }

  public teamForId(id: string): Observable<ApiTeam> {
    return this.availableTeams.pipe(map((teams) => teams.find((t) => t.teamId === id)));
  }

  private fetchData() {
    return {
      id: this.form.get('id')?.value,
      name: this.form.get('name').value,
      aggregation: this.form.get('aggregation').value,
      teams: this.form.get('teams').value,
    } as ApiTeamGroup;
  }

  public save() {
    if (this.form.valid) {
      this.dialogRef.close(this.fetchData());
    } else {
    }
  }

  public close() {
    this.dialogRef.close(null);
  }
}
