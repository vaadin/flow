/**
 * This module is generated from ComplexTypeParamsService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ComplexTypeParamsService
 */

// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Account';
import Group from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Group';

export function getComplexTypeParams(
  accounts: Array<Account>,
  groups: { [key: string]: Group; }
): Promise<void> {
  return client.call('ComplexTypeParamsService', 'getComplexTypeParams', {accounts, groups});
}