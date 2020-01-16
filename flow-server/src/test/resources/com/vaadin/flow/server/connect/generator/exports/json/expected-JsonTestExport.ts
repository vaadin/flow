/**
 * This class is used for OpenApi generator test
 *
 * This module is generated from JsonTestExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module JsonTestExport
 */

// @ts-ignore
import client from './connect-client.default';
import Version from './com/fasterxml/jackson/core/Version';
import Status from './com/vaadin/flow/server/connect/generator/exports/json/JsonTestExport/Status';
import User from './com/vaadin/flow/server/connect/generator/exports/json/JsonTestExport/User';

/**
 * Get number of users
 *
 * Return number of user
 */
export function countUser(): Promise<number> {
  return client.call('JsonTestExport', 'countUser');
}

/**
 * Get instant nano
 *
 * @param input input parameter
 * Return current time as an Instant
 */
export function fullFQNMethod(
  input: number
): Promise<string> {
  return client.call('JsonTestExport', 'fullFQNMethod', {input});
}

/**
 * Get the map of user and roles
 *
 * Return map of user and roles
 */
export function getAllUserRolesMap(): Promise<{ [key: string]: User; }> {
  return client.call('JsonTestExport', 'getAllUserRolesMap');
}

/**
 * Get all users
 *
 * Return list of users
 */
export function getAllUsers(): Promise<Array<User>> {
  return client.call('JsonTestExport', 'getAllUsers');
}

/**
 * Get array int
 *
 * @param input input string array
 * Return array of int
 */
export function getArrayInt(
  input: Array<string>
): Promise<Array<number>> {
  return client.call('JsonTestExport', 'getArrayInt', {input}, {requireCredentials: false});
}

/**
 * Get boolean value
 *
 * @param input input map
 * Return boolean value
 */
export function getBooleanValue(
  input: { [key: string]: User; }
): Promise<boolean> {
  return client.call('JsonTestExport', 'getBooleanValue', {input});
}

/**
 * Two parameters input method
 *
 * @param input first input description
 * @param secondInput second input description
 * Return boolean value
 */
export function getTwoParameters(
  input: string,
  secondInput: number
): Promise<boolean> {
  return client.call('JsonTestExport', 'getTwoParameters', {input, secondInput}, {requireCredentials: false});
}

/**
 * Get user by id
 *
 * @param id id of user
 * Return user with given id
 */
export function getUserById(
  id: number
): Promise<User> {
  return client.call('JsonTestExport', 'getUserById', {id}, {requireCredentials: false});
}

export function inputBeanTypeDependency(
  input: Version
): Promise<void> {
  return client.call('JsonTestExport', 'inputBeanTypeDependency', {input});
}

export function inputBeanTypeLocal(
  input: Status
): Promise<void> {
  return client.call('JsonTestExport', 'inputBeanTypeLocal', {input});
}

export function optionalParameter(
  parameter?: Array<string>,
  requiredParameter: string
): Promise<void> {
  return client.call('JsonTestExport', 'optionalParameter', {parameter, requiredParameter});
}

export function optionalReturn(): Promise<User | undefined> {
  return client.call('JsonTestExport', 'optionalReturn');
}

export function reservedWordInParameter(
  _delete: boolean
): Promise<void> {
  return client.call('JsonTestExport', 'reservedWordInParameter', {_delete});
}

/**
 * Update a user
 *
 * @param user User to be updated
 *
 */
export function updateUser(
  user: User
): Promise<void> {
  return client.call('JsonTestExport', 'updateUser', {user});
}