/**
 * This module is generated from ComplexHierarchyService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ComplexHierarchyService
 */

// @ts-ignore
import client from './connect-client.default';
import Model from './com/vaadin/flow/server/connect/generator/services/complexhierarchymodel/Model';

export function getModel(): Promise<Model> {
  return client.call('ComplexHierarchyService', 'getModel');
}