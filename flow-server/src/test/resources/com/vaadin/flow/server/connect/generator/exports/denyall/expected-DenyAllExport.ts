/**
 * This module is generated from DenyAllExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module DenyAllExport
 */

// @ts-ignore
import client from './connect-client.default';

export function shouldBeDisplayed1(): Promise<void> {
  return client.call('DenyAllExport', 'shouldBeDisplayed1');
}

export function shouldBeDisplayed2(): Promise<void> {
  return client.call('DenyAllExport', 'shouldBeDisplayed2');
}

export function shouldBeDisplayed3(): Promise<void> {
  return client.call('DenyAllExport', 'shouldBeDisplayed3', undefined, {requireCredentials: false});
}