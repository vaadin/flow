/**
 * This module is generated from ModelExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ModelExport
 */

// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/exports/model/ModelExport/Account';
import Group from './com/vaadin/flow/server/connect/generator/exports/model/ModelExport/Group';
import ModelFromDifferentPackage from './com/vaadin/flow/server/connect/generator/exports/model/subpackage/ModelFromDifferentPackage';

export function getAccountByGroups(
  groups: Array<Group>
): Promise<Account> {
  return client.call('ModelExport', 'getAccountByGroups', {groups});
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
  return client.call('ModelExport', 'getAccountByUserName', {userName});
}

export function getArrayOfAccount(): Promise<Array<Account>> {
  return client.call('ModelExport', 'getArrayOfAccount');
}

export function getMapGroups(): Promise<{ [key: string]: Group; }> {
  return client.call('ModelExport', 'getMapGroups');
}

/**
 * The import path of this model should be correct.
 *
 *
 */
export function getModelFromDifferentPackage(): Promise<ModelFromDifferentPackage> {
  return client.call('ModelExport', 'getModelFromDifferentPackage');
}