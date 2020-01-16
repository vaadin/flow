/**
 * This class is used for OpenApi generator test
 *
 * This module is generated from FooBarExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module FooBarExport
 */

// @ts-ignore
import client from './connect-client.default';

export function firstMethod(
  value?: boolean
): Promise<void> {
  return client.call('FooBarExport', 'firstMethod', {value}, {requireCredentials: false});
}
