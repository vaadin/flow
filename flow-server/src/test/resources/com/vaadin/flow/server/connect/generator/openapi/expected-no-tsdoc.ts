/**
 * This class is used for OpenApi generator test
 *
 * This module has been generated from GeneratorTestClass.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file.
 * @module GeneratorTestClass
 */

// @ts-ignore
import client from './connect-client.default';

export function getAllUsers(): Promise<void> {
  return client.call('GeneratorTestClass', 'getAllUsers', undefined, {requireCredentials: false});
}
