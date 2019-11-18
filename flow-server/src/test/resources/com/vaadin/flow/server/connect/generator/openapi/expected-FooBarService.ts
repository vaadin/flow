/**
 * This class is used for OpenApi generator test
 *
 * This module has been generated from FooBarService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file.
 * @module FooBarService
 */

// @ts-ignore
import client from './connect-client.default';

export function firstMethod(
  value?: boolean
): Promise<void> {
  return client.call('FooBarService', 'firstMethod', {value}, {requireCredentials: false});
}
