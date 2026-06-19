import { ApiEvaluation, ApiEvaluationInfo, ApiEvaluationOverview, ApiEvaluationTemplate, ApiTeamTaskOverview, DownloadService } from "../../../openapi";

/**
 * Type guard for ApiEvaluationTemplate.
 * Ignores existence of created and modified administrative fields and checks only for functional properties
 * @param obj
 */
export function instanceOfTemplate(obj: any): obj is ApiEvaluationTemplate {
  const idCheck = 'templateId' in obj || 'id' in obj;
  const minimumPropsCheck =
    'name' in obj &&
    'description' in obj &&
    'taskTypes' in obj &&
    'taskGroups' in obj &&
    'tasks' in obj &&
    'teams' in obj &&
    'judges' in obj &&
    'teamGroups' in obj;
  return idCheck && minimumPropsCheck;
}

export function instanceOfEvaluation(obj: any): obj is ApiEvaluation{
  const idCheck = 'evaluationId' in obj || 'id' in obj
  const minimumPropsCheck = 'name' in obj && 'type' in obj && 'template' in obj && 'created' in obj && 'tasks' in obj
  return idCheck && minimumPropsCheck
}

/**
 * Returns a copy of `overview` with `teamOverview` inserted in place of the entry for the same
 * team (or appended, if no such entry exists yet), leaving every other team's overview untouched.
 *
 * Used to apply the single-team overview diffs carried by TASK_UPDATED WebSocket messages without
 * having to refetch every team's overview just because one team submitted.
 */
export function mergeTeamOverview(overview: ApiEvaluationOverview, teamOverview: ApiTeamTaskOverview): ApiEvaluationOverview {
  const exists = overview.teamOverviews.some((t) => t.teamId === teamOverview.teamId);
  const teamOverviews = exists
    ? overview.teamOverviews.map((t) => (t.teamId === teamOverview.teamId ? teamOverview : t))
    : [...overview.teamOverviews, teamOverview];
  return { ...overview, teamOverviews };
}
