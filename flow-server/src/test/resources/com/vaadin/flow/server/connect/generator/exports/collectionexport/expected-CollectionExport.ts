/**
 * This module is generated from CollectionExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module CollectionExport
 */

// @ts-ignore
import client from './connect-client.default';
import Collection from './com/vaadin/flow/server/connect/generator/exports/collectionexport/CollectionExport/Collection';

/**
 * Get a collection by author name. The generator should not mix this type with the Java's Collection type.
 *
 * @param name author name
 * Return a collection
 */
export function getCollectionByAuthor(
  name: string
): Promise<Collection> {
  return client.call('CollectionExport', 'getCollectionByAuthor', {name});
}

/**
 * Get a list of user name.
 *
 * Return list of user name
 */
export function getListOfUserName(): Promise<Array<string>> {
  return client.call('CollectionExport', 'getListOfUserName');
}