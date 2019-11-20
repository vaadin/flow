/**
 * This module is generated from MyFirstTsClass.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module MyFirstTsClass
 */

// @ts-ignore
import client from './connect-client.default';
import User from './User';

/**
 * Get all users
 *
 * Return list of users
 */
export function getAllUsers(): Promise<Array<User>> {
  return client.call('GeneratorTestClass', 'getAllUsers', undefined, {requireCredentials: false});
}