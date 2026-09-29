/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { ReactiveController } from 'lit';
import type { Cache } from './cache.js';
import type { getFlatIndexByPath, getFlatIndexContext, getItemContext } from './helpers.js';

type DataProviderDefaultParams = {
  page: number;
  pageSize: number;
  parentItem?: unknown;
};

export type DataProviderCallback<TItem> = (items: TItem[], size?: number) => void;

export type DataProvider<TItem, TDataProviderParams extends Record<string, unknown>> = (
  params: DataProviderDefaultParams & TDataProviderParams,
  callback: DataProviderCallback<TItem>,
) => void;

/**
 * A controller that stores and manages items loaded with a data provider.
 */
export class DataProviderController<TItem, TDataProviderParams extends Record<string, unknown>>
  implements ReactiveController
{
  /**
   * The controller host element.
   */
  host: HTMLElement;

  /**
   * A callback that returns data based on the passed params such as
   * `page`, `pageSize`, `parentItem`, etc.
   */
  dataProvider: DataProvider<TItem, TDataProviderParams>;

  /**
   * A callback that returns additional params that need to be passed
   * to the data provider callback with every request.
   */
  dataProviderParams: () => TDataProviderParams;

  /**
   * A number of items to display per page.
   */
  pageSize: number;

  /**
   * A callback that returns whether the given item is expanded.
   */
  isExpanded: (item: TItem) => boolean;

  /**
   * A callback that returns the id for the given item and that
   * is used when checking object items for equality.
   */
  getItemId: (item: TItem) => unknown;

  /**
   * A reference to the root cache instance.
   */
  rootCache: Cache<TItem>;

  /**
   * A placeholder item that is used to indicate that the item is not loaded yet.
   */
  placeholder?: unknown;

  /**
   * A callback that returns whether the given item is a placeholder.
   */
  isPlaceholder?: (item: unknown) => boolean;

  constructor(
    host: HTMLElement,
    config: {
      size?: number;
      pageSize: number;
      placeholder?: unknown;
      isPlaceholder?(item: unknown): boolean;
      getItemId(item: TItem): unknown;
      isExpanded(item: TItem): boolean;
      dataProvider: DataProvider<TItem, TDataProviderParams>;
      dataProviderParams(): TDataProviderParams;
    },
  );

  /**
   * The total number of items, including items from expanded sub-caches.
   */
  get flatSize(): number;

  hostConnected(): void;

  hostDisconnected(): void;

  /**
   * Whether the root cache or any of its decendant caches have pending requests.
   */
  isLoading(): boolean;

  /**
   * Sets the size for the root cache and recalculates the flattened size.
   */
  setSize(size: number): void;

  /**
   * Sets the page size and clears the cache.
   */
  setPageSize(pageSize: number): void;

  /**
   * Sets the data provider callback and clears the cache.
   */
  setDataProvider(dataProvider: DataProvider<TItem, TDataProviderParams>): void;

  /**
   * Recalculates the flattened size.
   */
  recalculateFlatSize(): void;

  /**
   * Clears the cache.
   */
  clearCache(): void;

  /**
   * Returns context for the given flattened index, including:
   * - the corresponding cache
   * - the cache level
   * - the corresponding item (if loaded)
   * - the item's index in the cache's items array
   * - the page containing the item
   */
  getFlatIndexContext(flatIndex: number): ReturnType<typeof getFlatIndexContext<TItem>>;

  /**
   * Returns context for the given item, including:
   * - the cache containing the item
   * - the cache level
   * - the item
   * - the item's index in the cache's items array
   * - the item's flattened index
   * - the item's sub-cache (if exists)
   * - the page containing the item
   *
   * If the item isn't found, the method returns undefined.
   */
  getItemContext(item: TItem): ReturnType<typeof getItemContext<TItem>>;

  /**
   * Returns the flattened index for the item that the given indexes point to.
   * Each index in the path array points to a sub-item of the previous index.
   * Using `Infinity` as an index will point to the last item on the level.
   */
  getFlatIndexByPath(path: number[]): ReturnType<typeof getFlatIndexByPath<TItem>>;

  /**
   * Requests the data provider to load the page with the item corresponding
   * to the given flattened index. If the item is already loaded, the method
   * returns immediatelly.
   */
  ensureFlatIndexLoaded(flatIndex: number): void;

  /**
   * Creates a sub-cache for the item corresponding to the given flattened index and
   * requests the data provider to load the first page into the created sub-cache.
   * If the sub-cache already exists, the method returns immediatelly.
   */
  ensureFlatIndexHierarchy(flatIndex: number): void;

  /**
   * Loads the first page into the root cache.
   */
  loadFirstPage(): void;
}
