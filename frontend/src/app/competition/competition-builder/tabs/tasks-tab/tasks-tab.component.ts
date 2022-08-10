import {Component, OnInit, ViewChild} from '@angular/core';
import {RestTaskDescription, TaskGroup, TaskType} from '../../../../../../openapi';
import {EditableTaskComponent} from '../../components/editable-task/editable-task.component';

@Component({
  selector: 'app-tasks-tab',
  templateUrl: './tasks-tab.component.html',
  styleUrls: ['./tasks-tab.component.scss']
})
export class TasksTabComponent implements OnInit {

  @ViewChild('taskEditor', {static: true})
  taskEditor: EditableTaskComponent;

  constructor() { }

  ngOnInit(): void {
  }

  public editTask( taskType: TaskType, taskGroup: TaskGroup,task?: RestTaskDescription){
    this.taskEditor.taskType = taskType;
    this.taskEditor.taskGroup = taskGroup;
    this.taskEditor.task = task;
    this.taskEditor.init();
  }

}
