import { ApiEvaluation, ApiEvaluationInfo, ApiEvaluationOverview, ApiEvaluationTemplate, DownloadService } from "../../../openapi";

/**
 * Type guard for ApiEvaluationTemplate.
 * Ignores existence of created and modified administrative fields and checks only for functional properties
 * @param obj
 */
export function instanceOfTemplate(obj: any): obj is ApiEvaluationTemplate{
  const idCheck = 'templateId' in obj || 'id' in obj
  const minimumPropsCheck = 'name' in obj && 'description' in obj && 'taskTypes' in obj && 'taskGroups' in obj && 'tasks' in obj && 'teams' in obj && 'judges' in obj && 'teamGroups' in obj
  return idCheck && minimumPropsCheck
}

export function instanceOfEvaluation(obj: any): obj is ApiEvaluation{
  const idCheck = 'evaluationId' in obj || 'id' in obj
  const minimumPropsCheck = 'name' in obj && 'type' in obj && 'template' in obj && 'created' in obj && 'tasks' in obj
  return idCheck && minimumPropsCheck
}
