/**
 * This module is generated from ComplexTypeParamsExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ComplexTypeParamsExport
 */

// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/exports/model/ModelExport/Account';
import Group from './com/vaadin/flow/server/connect/generator/exports/model/ModelExport/Group';

export function getComplexTypeParams(
  accounts: Array<Account>,
  groups: { [key: string]: Group; }
): Promise<void> {
  return client.call('ComplexTypeParamsExport', 'getComplexTypeParams', {accounts, groups});
}