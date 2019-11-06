// @ts-ignore
import client from './connect-client.default';
import SameClassNameModel from './com/vaadin/flow/server/connect/generator/services/sameclassname/SameClassNameService/SameClassNameModel';
import SubpackageSameClassNameModel from './com/vaadin/flow/server/connect/generator/services/sameclassname/subpackage/SameClassNameModel';

export function getMyClass(
  sameClassNameModel: Array<SubpackageSameClassNameModel>
): Promise<SameClassNameModel> {
  return client.call('SameClassNameService', 'getMyClass', {sameClassNameModel});
}

export function getSubpackageModelList(
  sameClassNameModel: { [key: string]: SubpackageSameClassNameModel; }
): Promise<Array<SubpackageSameClassNameModel>> {
  return client.call('SameClassNameService', 'getSubpackageModelList', {sameClassNameModel});
}

export function getSubpackageModelMap(
  sameClassNameModel: { [key: string]: SameClassNameModel; }
): Promise<{ [key: string]: SubpackageSameClassNameModel; }> {
  return client.call('SameClassNameService', 'getSubpackageModelMap', {sameClassNameModel});
}

export function getSubpackageModel(): Promise<SubpackageSameClassNameModel> {
  return client.call('SameClassNameService', 'getSubpackageModel');
}

export function setSubpackageModel(
  model: SubpackageSameClassNameModel
): Promise<void> {
  return client.call('SameClassNameService', 'setSubpackageModel', {model});
}