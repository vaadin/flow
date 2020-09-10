/**
 * This module is generated from WholeClassDeferrableEndpoint.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module WholeClassDeferrableEndpoint
 */

// @ts-ignore
import client from './connect-client.default';
import { DeferrableResult } from '@vaadin/flow-frontend/Connect';

function _hello(
  userName: string
): Promise<DeferrableResult<string>> {
  return client.deferrableCall('WholeClassDeferrableEndpoint', 'hello', {userName});
}
export {_hello as hello};

function _hi(
  userName: string
): Promise<DeferrableResult<string>> {
  return client.deferrableCall('WholeClassDeferrableEndpoint', 'hi', {userName});
}
export {_hi as hi};
