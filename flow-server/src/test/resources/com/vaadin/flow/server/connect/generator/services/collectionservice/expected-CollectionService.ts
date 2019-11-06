// @ts-ignore
import * as connect from './connect-client.default';
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
  return connect.client.call('CollectionService', 'getCollectionByAuthor', {name});
}

/**
 * Get a list of user name.
 *
 * Return list of user name
 */
export function getListOfUserName(): Promise<Array<string>> {
  return connect.client.call('CollectionService', 'getListOfUserName');
}