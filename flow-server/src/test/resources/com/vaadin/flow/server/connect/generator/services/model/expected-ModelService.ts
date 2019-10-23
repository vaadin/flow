// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Account';
import Group from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Group';
import ModelFromDifferentPackage from './com/vaadin/flow/server/connect/generator/services/model/subpackage/ModelFromDifferentPackage';

export function getAccountByGroups(
  groups: Array<Group | null> | null
): Promise<Account | null> {
  return client.call('ModelService', 'getAccountByGroups', {groups});
}

/**
 * Get account by username.
 *
 * @param userName username of the account
 * Return the account with given userName
 */
export function getAccountByUserName(
  userName: string | null
): Promise<Account | null> {
  return client.call('ModelService', 'getAccountByUserName', {userName});
}

export function getArrayOfAccount(): Promise<Array<Account | null> | null> {
  return client.call('ModelService', 'getArrayOfAccount');
}

export function getMapGroups(): Promise<{ [key: string]: Group | null; } | null> {
  return client.call('ModelService', 'getMapGroups');
}

/**
 * The import path of this model should be correct.
 *
 *
 */
export function getModelFromDifferentPackage(): Promise<ModelFromDifferentPackage | null> {
  return client.call('ModelService', 'getModelFromDifferentPackage');
}
