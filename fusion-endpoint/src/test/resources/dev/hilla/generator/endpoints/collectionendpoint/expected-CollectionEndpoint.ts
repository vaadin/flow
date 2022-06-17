/**
 * This module is generated from CollectionEndpoint.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module CollectionEndpoint
 */

// @ts-ignore
import client from './connect-client.default';
// @ts-ignore
import { EndpointRequestInit, Subscription } from '@hilla/frontend';

import type Collection from './dev/hilla/generator/endpoints/collectionendpoint/CollectionEndpoint/Collection';

/**
 * Get a collection by author name. The generator should not mix this type with the Java's Collection type.
 *
 * @param name author name
 * @param init an optional object containing additional parameters for the request
 * Return a collection
 */
function _getCollectionByAuthor(
  name: string | undefined,
  init?: EndpointRequestInit
): Promise<Collection | undefined> {
  return client.call('CollectionEndpoint', 'getCollectionByAuthor', {name}, init);
}

/**
 * Get a list of user name.
 *
 * @param init an optional object containing additional parameters for the request
 * Return list of user name
 */
function _getListOfUserName(init?: EndpointRequestInit): Promise<Array<string | undefined> | undefined> {
  return client.call('CollectionEndpoint', 'getListOfUserName', {}, init);
}

export {
  _getCollectionByAuthor as getCollectionByAuthor,
  _getListOfUserName as getListOfUserName,
};
