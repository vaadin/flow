/**
 * This module is generated from InheritedModelService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module InheritedModelService
 */

// @ts-ignore
import client from './connect-client.default';
import ChildModel from './com/vaadin/flow/server/connect/generator/services/inheritedmodel/InheritedModelService/ChildModel';
import ParentModel from './com/vaadin/flow/server/connect/generator/services/inheritedmodel/InheritedModelService/ParentModel';

export function getParentModel(
  child: ChildModel
): Promise<ParentModel> {
  return client.call('InheritedModelService', 'getParentModel', {child});
}