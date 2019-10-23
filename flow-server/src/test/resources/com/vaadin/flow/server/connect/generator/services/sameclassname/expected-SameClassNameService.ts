// @ts-ignore
import client from './connect-client.default';
import SameClassNameModel from './com/vaadin/flow/server/connect/generator/services/sameclassname/SameClassNameService/SameClassNameModel';
import SubpackageSameClassNameModel from './com/vaadin/flow/server/connect/generator/services/sameclassname/subpackage/SameClassNameModel';

export function getMyClass(
  sameClassNameModel: Array<SubpackageSameClassNameModel | null> | null
): Promise<SameClassNameModel | null> {
  return client.call('SameClassNameService', 'getMyClass', {sameClassNameModel});
}

export function getSubpackageModelList(
  sameClassNameModel: { [key: string]: SubpackageSameClassNameModel | null; } | null
): Promise<Array<SubpackageSameClassNameModel | null> | null> {
  return client.call('SameClassNameService', 'getSubpackageModelList', {sameClassNameModel});
}

export function getSubpackageModelMap(
  sameClassNameModel: { [key: string]: SameClassNameModel | null; } | null
): Promise<{ [key: string]: SubpackageSameClassNameModel | null; } | null> {
  return client.call('SameClassNameService', 'getSubpackageModelMap', {sameClassNameModel});
}

export function getSubpackageModel(): Promise<SubpackageSameClassNameModel | null> {
  return client.call('SameClassNameService', 'getSubpackageModel');
}

export function setSubpackageModel(
  model: SubpackageSameClassNameModel | null
): Promise<void> {
  return client.call('SameClassNameService', 'setSubpackageModel', {model});
}
