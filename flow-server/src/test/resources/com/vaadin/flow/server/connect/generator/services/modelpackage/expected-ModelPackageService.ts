/**
 * This module is generated from ModelPackageService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module ModelPackageService
 */

// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/services/modelpackage/ModelPackageService/Account';

/**
 * Get a list of user name.
 *
 * Return list of user name
 */
export function getListOfUserName(): Promise<Array<string>> {
  return client.call('ModelPackageService', 'getListOfUserName');
}

/**
 * Get a collection by author name. The generator should not mix this type with the Java's Collection type.
 *
 * @param name author name
 * Return a collection
 */
export function getSameModelPackage(
  name: string
): Promise<Account> {
  return client.call('ModelPackageService', 'getSameModelPackage', {name});
}