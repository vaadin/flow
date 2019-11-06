/**
 * This class is used for OpenApi generator test
 *
 * This module has been generated from FooBarService.java
 * @module FooBarService
 */

// @ts-ignore
import * as connect from './connect-client.default';

export function firstMethod(
  value?: boolean
): Promise<void> {
  return connect.client.call('FooBarService', 'firstMethod', {value: connect.nullIfUndefined(value)}, {requireCredentials: false});
}