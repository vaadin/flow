/**
 * This module is generated from CollectionService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module CollectionService
 */

// @ts-ignore
import client from './connect-client.default';
import Collection from './com/vaadin/flow/server/connect/generator/services/collectionservice/CollectionService/Collection';

/**
 * Get a collection by author name. The generator should not mix this type with the Java's Collection type.
 *
 * @param name author name
 * Return a collection
 */
export function getCollectionByAuthor(
  name: string
): Promise<Collection> {
  return client.call('CollectionService', 'getCollectionByAuthor', {name});
}

/**
 * Get a list of user name.
 *
 * Return list of user name
 */
export function getListOfUserName(): Promise<Array<string>> {
  return client.call('CollectionService', 'getListOfUserName');
}