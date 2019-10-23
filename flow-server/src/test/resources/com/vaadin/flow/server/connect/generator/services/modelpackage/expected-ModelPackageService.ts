// @ts-ignore
import client from './connect-client.default';
import Account from './com/vaadin/flow/server/connect/generator/services/modelpackage/ModelPackageService/Account';

/**
 * Get a list of user name.
 *
 * Return list of user name
 */
export function getListOfUserName(): Promise<Array<string | null> | null> {
  return client.call('ModelPackageService', 'getListOfUserName');
}

/**
 * Get a collection by author name. The generator should not mix this type with the Java's Collection type.
 *
 * @param name author name
 * Return a collection
 */
export function getSameModelPackage(
  name: string | null
): Promise<Account | null> {
  return client.call('ModelPackageService', 'getSameModelPackage', {name});
}
