// @ts-ignore
import * as connect from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Account';
import Group from './com/vaadin/flow/server/connect/generator/services/model/ModelService/Group';

export function getComplexTypeParams(
  accounts: Array<Account>,
  groups: { [key: string]: Group; }
): Promise<void> {
  return connect.client.call('ComplexTypeParamsService', 'getComplexTypeParams', {accounts, groups});
}