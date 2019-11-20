/**
 * This module is generated from SubModelPackageService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module SubModelPackageService
 */

// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/services/modelpackage/subpackage/Account';

export function getSubAccountPackage(
  name: string
): Promise<Account> {
  return client.call('SubModelPackageService', 'getSubAccountPackage', {name});
}