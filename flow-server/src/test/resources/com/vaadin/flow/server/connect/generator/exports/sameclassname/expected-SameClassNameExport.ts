/**
 * This module is generated from SameClassNameExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module SameClassNameExport
 */

// @ts-ignore
import client from './connect-client.default';
import SameClassNameModel from './com/vaadin/flow/server/connect/generator/exports/sameclassname/SameClassNameExport/SameClassNameModel';
import SubpackageSameClassNameModel from './com/vaadin/flow/server/connect/generator/exports/sameclassname/subpackage/SameClassNameModel';

export function getMyClass(
  sameClassNameModel: Array<SubpackageSameClassNameModel>
): Promise<SameClassNameModel> {
  return client.call('SameClassNameExport', 'getMyClass', {sameClassNameModel});
}

export function getSubpackageModelList(
  sameClassNameModel: { [key: string]: SubpackageSameClassNameModel; }
): Promise<Array<SubpackageSameClassNameModel>> {
  return client.call('SameClassNameExport', 'getSubpackageModelList', {sameClassNameModel});
}

export function getSubpackageModelMap(
  sameClassNameModel: { [key: string]: SameClassNameModel; }
): Promise<{ [key: string]: SubpackageSameClassNameModel; }> {
  return client.call('SameClassNameExport', 'getSubpackageModelMap', {sameClassNameModel});
}

export function getSubpackageModel(): Promise<SubpackageSameClassNameModel> {
  return client.call('SameClassNameExport', 'getSubpackageModel');
}

export function setSubpackageModel(
  model: SubpackageSameClassNameModel
): Promise<void> {
  return client.call('SameClassNameExport', 'setSubpackageModel', {model});
}