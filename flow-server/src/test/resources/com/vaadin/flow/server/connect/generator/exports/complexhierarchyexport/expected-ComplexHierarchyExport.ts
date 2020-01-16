/**
 * This module is generated from ComplexHierarchyExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ComplexHierarchyExport
 */

// @ts-ignore
import client from './connect-client.default';
import Model from './com/vaadin/flow/server/connect/generator/exports/complexhierarchymodel/Model';

export function getModel(): Promise<Model> {
  return client.call('ComplexHierarchyExport', 'getModel');
}