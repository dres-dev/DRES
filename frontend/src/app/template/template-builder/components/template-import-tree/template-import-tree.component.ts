import { Component, Input, OnInit } from "@angular/core";
import {
  ApiEvaluationStatus,
  ApiEvaluationTemplate, ApiEvaluationTemplateOverview,
  ApiTaskGroup,
  ApiTaskTemplate,
  ApiTaskType,
  ApiTeam,
  ApiTeamGroup,
  ApiUser
} from "../../../../../../openapi";
import { FlatTreeControl } from "@angular/cdk/tree";
import { MatTreeFlatDataSource, MatTreeFlattener } from "@angular/material/tree";
import { SelectionModel } from "@angular/cdk/collections";

/* See https://v15.material.angular.io/components/tree/examples */

export enum TemplateImportTreeBranch {
  NONE = 0,                 // 000000
  TASK_TYPES = 1 << 0,      // 000001
  TASK_GROUPS = 1 << 1,     // 000010
  TASK_TEMPLATES = 1 << 2,  // 000100
  TEAMS = 1 << 3,           // 001000
  TEAM_GROUPS = 1 << 4,     // 010000
  JUDGES = 1 << 5,          // 100000
  ALL = ~(~0<<6)            // 111111
}

/**
 * Represents a flat node with possible expansion and its number.
 */
export class TemplateTreeFlatNode<T> {
  level: number;
  expandable: boolean;
  item: T;
  label: string;
  branch: TemplateImportTreeBranch
}

export class TemplateTreeNode<T> {
  children: TemplateTreeNode<ApiTaskType | ApiTaskGroup | ApiTaskTemplate | ApiTeam | ApiTeamGroup | ApiUser | ApiTaskType[] | ApiTaskGroup[] | ApiTaskTemplate[] | ApiTeam[] | ApiTeamGroup[] | ApiUser[]>[] | null;
  item: T;
  label: string;
  branch: TemplateImportTreeBranch;
  origin: string; // TemplateID
}

@Component({
  selector: "app-template-import-tree",
  templateUrl: "./template-import-tree.component.html",
  styleUrls: ["./template-import-tree.component.scss"]
})
export class TemplateImportTreeComponent implements OnInit{

  flatNodeMap = new Map<TemplateTreeFlatNode<any>, TemplateTreeNode<any>>();
  nestedNodeMap = new Map<TemplateTreeNode<any>, TemplateTreeFlatNode<any>>();

  templatesMap = new Map<string, ApiEvaluationTemplate>();

  selectedParent: TemplateTreeFlatNode<any> | null = null;

  treeControl: FlatTreeControl<TemplateTreeFlatNode<any>>
  treeFlattener: MatTreeFlattener<TemplateTreeNode<any>, TemplateTreeFlatNode<any>>
  dataSource: MatTreeFlatDataSource<TemplateTreeNode<any>, TemplateTreeFlatNode<any>>

  selection = new SelectionModel<TemplateTreeFlatNode<any>>(true);

  @Input()
  templates: ApiEvaluationTemplate[];
  @Input()
  branches: TemplateImportTreeBranch;

  constructor() {
    this.treeFlattener = new MatTreeFlattener<TemplateTreeNode<any>, TemplateTreeFlatNode<any>>(
      this.transformer, this.getLevel, this.isExpandable, this.getChildren
    )
    this.treeControl = new FlatTreeControl<TemplateTreeFlatNode<any>>(this.getLevel, this.isExpandable);
    this.dataSource = new MatTreeFlatDataSource<TemplateTreeNode<any>, TemplateTreeFlatNode<any>>(this.treeControl, this.treeFlattener);

  }

  ngOnInit(): void {
      this.dataSource.data = TemplateImportTreeComponent.buildTrees(this.templates, this.branches);
      this.templates.forEach(it => this.templatesMap.set(it.id, it));
    }

  getLevel = (node: TemplateTreeFlatNode<any>) => node.level;
  isExpandable = (node: TemplateTreeFlatNode<any>) => node.expandable;
  getChildren = (node: TemplateTreeNode<any>) => node.children;
  hasChild = (_: number, node: TemplateTreeFlatNode<any>) => node.expandable

  transformer = (node: TemplateTreeNode<any>, level: number) => {
    const existingNode = this.nestedNodeMap.get(node);
    const flatNode = existingNode && existingNode.item === node.item ? existingNode : new TemplateTreeFlatNode<any>();
    flatNode.item = node.item;
    flatNode.level = level;
    flatNode.expandable = !!node.children?.length;
    flatNode.branch = node.branch;
    flatNode.label = node.label;
    this.flatNodeMap.set(flatNode, node);
    this.nestedNodeMap.set(node, flatNode);
    return flatNode;
  }

  /** Whether all the descendants of the node are selected. */
  descendantsAllSelected(node: TemplateTreeFlatNode<any>): boolean {
    const descendants = this.treeControl.getDescendants(node);
    return descendants.length > 0 &&
      descendants.every(child => {
        return this.selection.isSelected(child);
      });
  }

  /** Whether part of the descendants are selected */
  descendantsPartiallySelected(node: TemplateTreeFlatNode<any>): boolean {
    const descendants = this.treeControl.getDescendants(node);
    const result = descendants.some(child => this.selection.isSelected(child));
    return result && !this.descendantsAllSelected(node);
  }

  /** Toggle the to-do item selection. Select/deselect all the descendants node */
  itemSelectionToggle(node: TemplateTreeFlatNode<any>): void {
    this.selection.toggle(node);
    const descendants = this.treeControl.getDescendants(node);
    this.selection.isSelected(node)
      ? this.selection.select(...descendants)
      : this.selection.deselect(...descendants);

    // Force update for the parent
    descendants.forEach(child => this.selection.isSelected(child));
    this.checkAllParentsSelection(node);
  }

  /** Toggle a leaf to-do item selection. Check all the parents to see if they changed */
  leafItemSelectionToggle(node: TemplateTreeFlatNode<any>): void {
    this.selection.toggle(node);
    this.checkAllParentsSelection(node);
  }

  /* Checks all the parents when a leaf node is selected/unselected */
  checkAllParentsSelection(node: TemplateTreeFlatNode<any>): void {
    let parent: TemplateTreeFlatNode<any> | null = this.getParentNode(node);
    while (parent) {
      this.checkRootNodeSelection(parent);
      parent = this.getParentNode(parent);
    }
  }

  /** Check root node checked state and change it accordingly */
  checkRootNodeSelection(node: TemplateTreeFlatNode<any>): void {
    const nodeSelected = this.selection.isSelected(node);
    const descendants = this.treeControl.getDescendants(node);
    const descAllSelected =
      descendants.length > 0 &&
      descendants.every(child => {
        return this.selection.isSelected(child);
      });
    if (nodeSelected && !descAllSelected) {
      this.selection.deselect(node);
    } else if (!nodeSelected && descAllSelected) {
      this.selection.select(node);
    }
  }

  /* Get the parent node of a node */
  getParentNode(node: TemplateTreeFlatNode<any>): TemplateTreeFlatNode<any> | null {
    const currentLevel = this.getLevel(node);

    if (currentLevel < 1) {
      return null;
    }

    const startIndex = this.treeControl.dataNodes.indexOf(node) - 1;

    for (let i = startIndex; i >= 0; i--) {
      const currentNode = this.treeControl.dataNodes[i];

      if (this.getLevel(currentNode) < currentLevel) {
        return currentNode;
      }
    }
    return null;
  }

  public getImportTemplate(){
    const template = {
      name: "<IMPORT-TEMPLATE>",
      description: "---Automatically generated template whose elements get imported. If this is seen, there was a programmer's error somewhere---",
      taskTypes: this.getAllSelectedTaskTypes(),
      taskGroups: this.getAllSelectedTaskGroups(),
      tasks: this.getAllSelectedTaskTemplates(),
      teams: this.getAllSelectedTeams(),
      teamGroups: this.getAllSelectedTeamGroups(),
      judges: this.getAllSelectedJudges(),
      id:"---IMPORT_TEMPLATE_NO_ID---"
    } as ApiEvaluationTemplate

    /* Sanitisation: For each task, the group and type is required */
    return template;
  }

  public getAllSelectedTaskTypes(){
    return this.getSelectedItemsForBranch(TemplateImportTreeBranch.TASK_TYPES) as ApiTaskType[]
  }

  public getAllSelectedTaskGroups(){
    return this.getSelectedItemsForBranch(TemplateImportTreeBranch.TASK_GROUPS) as ApiTaskGroup[];
  }

  public getAllSelectedTaskTemplates(){
    return this.getSelectedItemsForBranch(TemplateImportTreeBranch.TASK_TEMPLATES) as ApiTaskTemplate[]
  }

  public getAllSelectedTeams(){
    return this.getSelectedItemsForBranch(TemplateImportTreeBranch.TEAMS) as ApiTeam[]
  }

  public getAllSelectedTeamGroups(){
    return this.getSelectedItemsForBranch(TemplateImportTreeBranch.TEAM_GROUPS) as ApiTeamGroup[]
  }

  public getAllSelectedJudges(){
    return this.getSelectedItemsForBranch(TemplateImportTreeBranch.JUDGES) as ApiUser[]
  }

  /**
   *
   * @param branch A single branch, do not use ALL or NONE here (or any combination)
   * @private
   */
  private getSelectedItemsForBranch(branch: TemplateImportTreeBranch){
    /* Filter appropriately */
    const items = this.selection.selected.filter(it => TemplateImportTreeComponent.checkForBranch(it.branch, branch)).map(it => this.flatNodeMap.get(it))
    switch(branch){
      case TemplateImportTreeBranch.NONE:
      case TemplateImportTreeBranch.ALL:
        throw new Error("Cannot type set for TemplateImportTreeBanches ALL and NONE. This is a programmer's error")
      case TemplateImportTreeBranch.TASK_TYPES:
        return items.map<ApiTaskType>(it => it.item)
      case TemplateImportTreeBranch.TASK_GROUPS:
        return items.map<ApiTaskGroup>(it => it.item)
      case TemplateImportTreeBranch.TASK_TEMPLATES:
        return items.map<ApiTaskTemplate>(it => {
          /* Warning: collectionId remains and therefore must exist */
          const newItem = it.item as ApiTaskTemplate;
          newItem.id = undefined;
          return newItem
        })
      case TemplateImportTreeBranch.TEAMS:
        return items.map<ApiTeam>(it => {
          const newItem = it.item as ApiTeam
          newItem.id = undefined;
          return newItem
        })
      case TemplateImportTreeBranch.TEAM_GROUPS:
        return items.map<ApiTeamGroup>(it => {
          const newItem = it.item as ApiTeamGroup
          newItem.id = undefined
          return newItem
        })
      case TemplateImportTreeBranch.JUDGES:
        return items.map<ApiUser>(it => it.item)
    }
  }



  public static buildTrees(templates: ApiEvaluationTemplate[], branches: TemplateImportTreeBranch): TemplateTreeNode<ApiEvaluationTemplate>[]{
    return templates.map(it => this.buildTree(it, branches));
  }

  public static buildTree(template: ApiEvaluationTemplate, branches: TemplateImportTreeBranch): TemplateTreeNode<ApiEvaluationTemplate> {
    const root = new TemplateTreeNode<ApiEvaluationTemplate>();
    root.item = template;
    root.label = template.name;
    root.children = [] as TemplateTreeNode<any>[];
    if(this.checkForBranch(branches, TemplateImportTreeBranch.TASK_TYPES)){
      root.children.push(this.buildTaskTypesBranch(template));
    }
    if(this.checkForBranch(branches, TemplateImportTreeBranch.TASK_GROUPS)){
      root.children.push(this.buildTaskGroupsBranch(template));
    }
    if(this.checkForBranch(branches, TemplateImportTreeBranch.TASK_TEMPLATES)){
      root.children.push(this.buildTaskTemplatesBranch(template));
    }
    if(this.checkForBranch(branches, TemplateImportTreeBranch.TEAMS)){
      root.children.push(this.buildTeamsBranch(template));
    }
    if(this.checkForBranch(branches, TemplateImportTreeBranch.TEAM_GROUPS)){
      root.children.push(this.buildTeamGroupsBranch(template));
    }
    if(this.checkForBranch(branches, TemplateImportTreeBranch.TEAM_GROUPS)){
      root.children.push(this.buildJudgesBranch(template));
    }
    return root;
  }

  public static checkForBranch(branches: TemplateImportTreeBranch, test: TemplateImportTreeBranch): boolean{
    return (branches & test) === test
  }

  public static buildTaskTypesBranch(template: ApiEvaluationTemplate): TemplateTreeNode<ApiTaskType[]> {
    return this.buildBranch<ApiTaskType>(template, "taskTypes", "Task Types", TemplateImportTreeBranch.TASK_TYPES);
  }

  public static buildTaskGroupsBranch(template: ApiEvaluationTemplate): TemplateTreeNode<ApiTaskGroup[]> {
    return this.buildBranch<ApiTaskGroup>(template, "taskGroups", "Task Groups", TemplateImportTreeBranch.TASK_GROUPS);
  }

  public static buildTaskTemplatesBranch(template: ApiEvaluationTemplate): TemplateTreeNode<ApiTaskTemplate[]> {
    return this.buildBranch<ApiTaskTemplate>(template, "tasks", "Task Templates", TemplateImportTreeBranch.TASK_TEMPLATES);
  }

  public static buildTeamsBranch(template: ApiEvaluationTemplate): TemplateTreeNode<ApiTeam[]> {
    return this.buildBranch<ApiTeam>(template, "teams", "Teams", TemplateImportTreeBranch.TEAMS);
  }

  public static buildTeamGroupsBranch(template: ApiEvaluationTemplate): TemplateTreeNode<ApiTeamGroup[]> {
    return this.buildBranch<ApiTeamGroup>(template, "teamGroups", "Team Groups", TemplateImportTreeBranch.TEAM_GROUPS);
  }

  public static buildJudgesBranch(template: ApiEvaluationTemplate): TemplateTreeNode<ApiUser[]> {
    return this.buildBranch<ApiUser>(template, "judges", "Judges", TemplateImportTreeBranch.JUDGES);
  }

  public static buildBranch<T>(template: ApiEvaluationTemplate, key: string, rootLabel: string, branch: TemplateImportTreeBranch): TemplateTreeNode<T[]> {
    const root = new TemplateTreeNode<T[]>();
    root.label = rootLabel;
    root.item = template[key];
    root.children = template[key].map(it => {
      const item = new TemplateTreeNode<T>();
      item.label = it["name"];
      item.item = it;
      item.children = null;
      item.branch = branch;
      item.origin = template.id
      return item;
    });
    return root;
  }

}
