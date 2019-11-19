// @ts-ignore
import client from './connect-client.default';
import Model from './com/vaadin/flow/server/connect/generator/services/complexhierarchymodel/Model';

export function getModel(): Promise<Model> {
  return client.call('ComplexHierarchyService', 'getModel');
}