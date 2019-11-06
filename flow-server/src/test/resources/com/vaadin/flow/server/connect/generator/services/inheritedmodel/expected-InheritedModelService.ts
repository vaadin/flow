// @ts-ignore
import client from './connect-client.default';
import ChildModel from './com/vaadin/flow/server/connect/generator/services/inheritedmodel/InheritedModelService/ChildModel';
import ParentModel from './com/vaadin/flow/server/connect/generator/services/inheritedmodel/InheritedModelService/ParentModel';

export function getParentModel(
  child: ChildModel
): Promise<ParentModel> {
  return client.call('InheritedModelService', 'getParentModel', {child});
}