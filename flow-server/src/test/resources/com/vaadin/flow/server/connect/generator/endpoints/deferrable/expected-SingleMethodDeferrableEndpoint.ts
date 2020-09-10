/**
 * This module is generated from SingleMethodDeferrableEndpoint.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module SingleMethodDeferrableEndpoint
 */

// @ts-ignore
import client from './connect-client.default';
import { DeferrableResult } from '@vaadin/flow-frontend/Connect';

function _hello(
  userName: string
): Promise<DeferrableResult<string>> {
  return client.deferrableCall('SingleMethodDeferrableEndpoint', 'hello', {userName});
}
export {_hello as hello};

function _hi(
  userName: string
): Promise<string> {
  return client.call('SingleMethodDeferrableEndpoint', 'hi', {userName});
}
export {_hi as hi};
