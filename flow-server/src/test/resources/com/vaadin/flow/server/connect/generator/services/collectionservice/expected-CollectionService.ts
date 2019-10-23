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
  name: string | null
): Promise<Collection | null> {
  return client.call('CollectionService', 'getCollectionByAuthor', {name});
}

/**
 * Get a list of user name.
 *
 * Return list of user name
 */
export function getListOfUserName(): Promise<Array<string | null> | null> {
  return client.call('CollectionService', 'getListOfUserName');
}