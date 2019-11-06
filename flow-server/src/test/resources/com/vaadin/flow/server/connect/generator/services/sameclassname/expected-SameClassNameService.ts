// @ts-ignore
import * as connect from './connect-client.default';
import SameClassNameModel from './com/vaadin/flow/server/connect/generator/services/sameclassname/SameClassNameService/SameClassNameModel';
import SubpackageSameClassNameModel from './com/vaadin/flow/server/connect/generator/services/sameclassname/subpackage/SameClassNameModel';

export function getMyClass(
  sameClassNameModel: Array<SubpackageSameClassNameModel>
): Promise<SameClassNameModel> {
  return connect.client.call('SameClassNameService', 'getMyClass', {sameClassNameModel});
}

export function getSubpackageModelList(
  sameClassNameModel: { [key: string]: SubpackageSameClassNameModel; }
): Promise<Array<SubpackageSameClassNameModel>> {
  return connect.client.call('SameClassNameService', 'getSubpackageModelList', {sameClassNameModel});
}

export function getSubpackageModelMap(
  sameClassNameModel: { [key: string]: SameClassNameModel; }
): Promise<{ [key: string]: SubpackageSameClassNameModel; }> {
  return connect.client.call('SameClassNameService', 'getSubpackageModelMap', {sameClassNameModel});
}

export function getSubpackageModel(): Promise<SubpackageSameClassNameModel> {
  return connect.client.call('SameClassNameService', 'getSubpackageModel');
}

export function setSubpackageModel(
  model: SubpackageSameClassNameModel
): Promise<void> {
  return connect.client.call('SameClassNameService', 'setSubpackageModel', {model});
}