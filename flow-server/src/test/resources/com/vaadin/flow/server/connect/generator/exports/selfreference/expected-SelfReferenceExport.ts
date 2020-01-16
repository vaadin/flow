/**
 * This module is generated from SelfReferenceExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module SelfReferenceExport
 */

// @ts-ignore
import client from './connect-client.default';
import SelfReference from './com/vaadin/flow/server/connect/generator/exports/selfreference/SelfReference';

export function getModel(): Promise<SelfReference> {
  return client.call('SelfReferenceExport', 'getModel');
}
