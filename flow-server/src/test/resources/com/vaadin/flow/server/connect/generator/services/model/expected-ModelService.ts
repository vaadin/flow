/**
 * This module is generated from ModelService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ModelService
 */

// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Account';
import Group from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Group';
import ModelFromDifferentPackage from './com/vaadin/flow/server/connect/generator/services/model/subpackage/ModelFromDifferentPackage';

export function getAccountByGroups(
  groups: Array<Group>
): Promise<Account> {
  return client.call('ModelService', 'getAccountByGroups', {groups});
}

/**
 * Get account by username.
 *
 * @param userName username of the account
 * Return the account with given userName
 */
export function getAccountByUserName(
  userName: string
): Promise<Account> {
  return client.call('ModelService', 'getAccountByUserName', {userName});
}

export function getArrayOfAccount(): Promise<Array<Account>> {
  return client.call('ModelService', 'getArrayOfAccount');
}

export function getMapGroups(): Promise<{ [key: string]: Group; }> {
  return client.call('ModelService', 'getMapGroups');
}

/**
 * The import path of this model should be correct.
 *
 *
 */
export function getModelFromDifferentPackage(): Promise<ModelFromDifferentPackage> {
  return client.call('ModelService', 'getModelFromDifferentPackage');
}