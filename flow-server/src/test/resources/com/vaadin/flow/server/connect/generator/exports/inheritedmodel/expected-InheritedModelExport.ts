/**
 * This module is generated from InheritedModelExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module InheritedModelExport
 */

// @ts-ignore
import client from './connect-client.default';
import ChildModel from './com/vaadin/flow/server/connect/generator/exports/inheritedmodel/InheritedModelExport/ChildModel';
import ParentModel from './com/vaadin/flow/server/connect/generator/exports/inheritedmodel/InheritedModelExport/ParentModel';

export function getParentModel(
  child: ChildModel
): Promise<ParentModel> {
  return client.call('InheritedModelExport', 'getParentModel', {child});
}