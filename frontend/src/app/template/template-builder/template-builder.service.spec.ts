import { TestBed } from '@angular/core/testing';
import { TemplateBuilderService } from './template-builder.service';
import { ApiEvaluationTemplate, ApiTaskGroup, ApiTaskTemplate, ApiTaskType } from '../../../../openapi';

function makeTemplate(overrides: Partial<ApiEvaluationTemplate> = {}): ApiEvaluationTemplate {
  return {
    taskTypes: [],
    taskGroups: [],
    tasks: [],
    teams: [],
    teamGroups: [],
    judges: [],
    ...overrides,
  } as ApiEvaluationTemplate;
}

describe('TemplateBuilderService', () => {
  let service: TemplateBuilderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TemplateBuilderService);
  });

  // --- removeTaskType ---

  describe('removeTaskType', () => {
    it('removes the task type by name', () => {
      service.initialise(makeTemplate({ taskTypes: [{ name: 'KIS' }, { name: 'AVS' }] as ApiTaskType[] }));
      service.removeTaskType({ name: 'KIS' } as ApiTaskType);
      expect(service.getTemplate().taskTypes.map((t) => t.name)).toEqual(['AVS']);
    });

    it('removes by name even when the object reference differs', () => {
      const original = { name: 'KIS' } as ApiTaskType;
      service.initialise(makeTemplate({ taskTypes: [original] }));
      const copy = { name: 'KIS' } as ApiTaskType; // different reference
      service.removeTaskType(copy);
      expect(service.getTemplate().taskTypes.length).toBe(0);
    });

    it('does not throw and does not corrupt the list when item is not found', () => {
      service.initialise(makeTemplate({ taskTypes: [{ name: 'AVS' }] as ApiTaskType[] }));
      service.removeTaskType({ name: 'Ghost' } as ApiTaskType);
      expect(service.getTemplate().taskTypes.length).toBe(1);
    });

    it('cascades removal to task groups of that type', () => {
      service.initialise(
        makeTemplate({
          taskTypes: [{ name: 'KIS' }] as ApiTaskType[],
          taskGroups: [
            { name: 'G1', type: 'KIS' },
            { name: 'G2', type: 'AVS' },
          ] as ApiTaskGroup[],
        })
      );
      service.removeTaskType({ name: 'KIS' } as ApiTaskType);
      expect(service.getTemplate().taskGroups.map((g) => g.name)).toEqual(['G2']);
    });

    it('cascades removal all the way to tasks', () => {
      service.initialise(
        makeTemplate({
          taskTypes: [{ name: 'KIS' }] as ApiTaskType[],
          taskGroups: [{ name: 'G1', type: 'KIS' }] as ApiTaskGroup[],
          tasks: [
            { name: 'T1', taskGroup: 'G1' },
            { name: 'T2', taskGroup: 'OTHER' },
          ] as ApiTaskTemplate[],
        })
      );
      service.removeTaskType({ name: 'KIS' } as ApiTaskType);
      expect(service.getTemplate().tasks.map((t) => t.name)).toEqual(['T2']);
    });
  });

  // --- removeTaskGroup ---

  describe('removeTaskGroup', () => {
    it('removes the group by id when id is present', () => {
      service.initialise(
        makeTemplate({
          taskGroups: [
            { id: '1', name: 'G1' },
            { id: '2', name: 'G2' },
          ] as ApiTaskGroup[],
        })
      );
      service.removeTaskGroup({ id: '1', name: 'G1' } as ApiTaskGroup);
      expect(service.getTemplate().taskGroups.map((g) => g.name)).toEqual(['G2']);
    });

    it('removes by id even when object reference differs', () => {
      service.initialise(
        makeTemplate({
          taskGroups: [{ id: '1', name: 'G1' }] as ApiTaskGroup[],
        })
      );
      service.removeTaskGroup({ id: '1', name: 'G1' } as ApiTaskGroup);
      expect(service.getTemplate().taskGroups.length).toBe(0);
    });

    it('falls back to name matching when no id', () => {
      service.initialise(
        makeTemplate({
          taskGroups: [{ name: 'G1' }, { name: 'G2' }] as ApiTaskGroup[],
        })
      );
      service.removeTaskGroup({ name: 'G1' } as ApiTaskGroup);
      expect(service.getTemplate().taskGroups.map((g) => g.name)).toEqual(['G2']);
    });

    it('cascades removal to tasks belonging to the group', () => {
      service.initialise(
        makeTemplate({
          taskGroups: [{ id: '1', name: 'G1' }] as ApiTaskGroup[],
          tasks: [
            { name: 'T1', taskGroup: 'G1' },
            { name: 'T2', taskGroup: 'G2' },
          ] as ApiTaskTemplate[],
        })
      );
      service.removeTaskGroup({ id: '1', name: 'G1' } as ApiTaskGroup);
      expect(service.getTemplate().tasks.map((t) => t.name)).toEqual(['T2']);
    });
  });

  // --- removeTask ---

  describe('removeTask', () => {
    it('removes the task by id when id is present', () => {
      service.initialise(
        makeTemplate({
          tasks: [
            { id: 'a', name: 'T1' },
            { id: 'b', name: 'T2' },
          ] as ApiTaskTemplate[],
        })
      );
      service.removeTask({ id: 'a', name: 'T1' } as ApiTaskTemplate);
      expect(service.getTemplate().tasks.map((t) => t.name)).toEqual(['T2']);
    });

    it('removes by id even when object reference differs', () => {
      service.initialise(
        makeTemplate({
          tasks: [{ id: 'a', name: 'T1' }] as ApiTaskTemplate[],
        })
      );
      service.removeTask({ id: 'a', name: 'T1' } as ApiTaskTemplate);
      expect(service.getTemplate().tasks.length).toBe(0);
    });

    it('falls back to reference equality when no id', () => {
      const task = { name: 'T1' } as ApiTaskTemplate;
      service.initialise(makeTemplate({ tasks: [task, { name: 'T2' } as ApiTaskTemplate] }));
      service.removeTask(task);
      expect(service.getTemplate().tasks.map((t) => t.name)).toEqual(['T2']);
    });

    it('deselects the task if it was selected', () => {
      const task = { name: 'T1', taskGroup: 'G1', taskType: 'KIS' } as ApiTaskTemplate;
      service.initialise(
        makeTemplate({
          taskTypes: [{ name: 'KIS' }] as ApiTaskType[],
          taskGroups: [{ name: 'G1', type: 'KIS' }] as ApiTaskGroup[],
          tasks: [task],
        })
      );
      service.selectTaskTemplate(task);
      expect(service.getSelectedTaskTemplate()).toBe(task);
      service.removeTask(task);
      expect(service.getSelectedTaskTemplate()).toBeNull();
    });
  });
});
