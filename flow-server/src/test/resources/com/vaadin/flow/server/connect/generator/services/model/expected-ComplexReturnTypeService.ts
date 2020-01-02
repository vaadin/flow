/**
 * This module is generated from ComplexReturnTypeService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ComplexReturnTypeService
 */

// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Account';

export function getAccounts(): Promise<Array<Account>> {
  return client.call('ComplexReturnTypeService', 'getAccounts');
}