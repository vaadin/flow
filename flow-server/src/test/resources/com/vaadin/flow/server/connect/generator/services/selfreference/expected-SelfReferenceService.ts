/**
 * This module is generated from SelfReferenceService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module SelfReferenceService
 */

// @ts-ignore
import client from './connect-client.default';
import SelfReference from './com/vaadin/flow/server/connect/generator/services/selfreference/SelfReference';

export function getModel(): Promise<SelfReference> {
  return client.call('SelfReferenceService', 'getModel');
}
