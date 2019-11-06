/**
 * This nested class is also used in the OpenApi generator test
 *
 * This module has been generated from GeneratorAnonymousAllowedTestClass.java
 * @module GeneratorAnonymousAllowedTestClass
 */

// @ts-ignore
import * as connect from './connect-client.default';

export function anonymousAllowed(): Promise<void> {
  return connect.client.call('customName', 'anonymousAllowed', undefined, {requireCredentials: false});
}

export function permissionAltered1(): Promise<void> {
  return connect.client.call('customName', 'permissionAltered1');
}

export function permissionAltered2(): Promise<void> {
  return connect.client.call('customName', 'permissionAltered2');
}
