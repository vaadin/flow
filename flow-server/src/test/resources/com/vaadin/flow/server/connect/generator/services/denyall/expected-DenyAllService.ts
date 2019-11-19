/**
 * This module is generated from DenyAllService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module DenyAllService
 */

// @ts-ignore
import client from './connect-client.default';

export function shouldBeDisplayed1(): Promise<void> {
  return client.call('DenyAllService', 'shouldBeDisplayed1');
}

export function shouldBeDisplayed2(): Promise<void> {
  return client.call('DenyAllService', 'shouldBeDisplayed2');
}

export function shouldBeDisplayed3(): Promise<void> {
  return client.call('DenyAllService', 'shouldBeDisplayed3', undefined, {requireCredentials: false});
}