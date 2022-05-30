/**
* This module is generated from PageEndpoint.java
* All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
* @module PageEndpoint
*/
// @ts-ignore
import client from './connect-client.default';
// @ts-ignore
import { EndpointRequestInit, Subscription } from '@hilla/frontend';
import type Foo from './dev/hilla/generator/endpoints/pageendpoint/PageEndpoint/Foo';
function _getPageOfObjects(endpointRequestInit?: EndpointRequestInit): Promise<Array<Foo | undefined> | undefined> {
  return client.call (
    'PageEndpoint', 'getPageOfObjects', {}, endpointRequestInit
  );
}
function _getPageOfStrings(endpointRequestInit?: EndpointRequestInit): Promise<Array<string | undefined> | undefined> {
  return client.call (
    'PageEndpoint', 'getPageOfStrings', {}, endpointRequestInit
  );
}
export {
  _getPageOfObjects as getPageOfObjects,
  _getPageOfStrings as getPageOfStrings,
};
