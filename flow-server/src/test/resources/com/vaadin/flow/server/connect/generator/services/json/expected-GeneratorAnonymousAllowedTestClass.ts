/**
 * This nested class is also used in the OpenApi generator test
 *
 * This module is generated from GeneratorAnonymousAllowedTestClass.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module GeneratorAnonymousAllowedTestClass
 */

// @ts-ignore
import client from './connect-client.default';

export function anonymousAllowed(): Promise<void> {
  return client.call('customName', 'anonymousAllowed', undefined, {requireCredentials: false});
}

export function permissionAltered1(): Promise<void> {
  return client.call('customName', 'permissionAltered1');
}

export function permissionAltered2(): Promise<void> {
  return client.call('customName', 'permissionAltered2');
}