/**
 * This module is generated from ComplexReturnTypeExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ComplexReturnTypeExport
 */

// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/exports/model/ModelExport/Account';

export function getAccounts(): Promise<Array<Account>> {
  return client.call('ComplexReturnTypeExport', 'getAccounts');
}