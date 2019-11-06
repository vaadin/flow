// @ts-ignore
import * as connect from './connect-client.default';
import User from './User';

/**
 * Get all users
 *
 * Return list of users
 */
export function getAllUsers(): Promise<Array<User>> {
  return connect.client.call('GeneratorTestClass', 'getAllUsers', undefined, {requireCredentials: false});
}
