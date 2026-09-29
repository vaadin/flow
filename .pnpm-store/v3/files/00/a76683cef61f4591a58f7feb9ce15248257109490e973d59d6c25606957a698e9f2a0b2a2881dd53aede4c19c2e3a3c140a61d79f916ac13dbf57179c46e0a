import { IronListAdapter } from './virtualizer-iron-list-adapter.js';

export class Virtualizer {
  /**
   * @typedef {Object} VirtualizerConfig
   * @property {Function} createElements Function that returns the given number of new elements
   * @property {Function} updateElement Function that updates the element at a specific index
   * @property {HTMLElement} scrollTarget Reference to the scrolling element
   * @property {HTMLElement} scrollContainer Reference to a wrapper for the item elements (or a slot) inside the scrollTarget
   * @property {HTMLElement | undefined} elementsContainer Reference to the container in which the item elements are placed, defaults to scrollContainer
   * @property {boolean | undefined} reorderElements Determines whether the physical item elements should be kept in order in the DOM
   * @param {VirtualizerConfig} config Configuration for the virtualizer
   */
  constructor(config) {
    this.__adapter = new IronListAdapter(config);
  }

  /**
   * Gets the index of the first visible item in the viewport.
   *
   * @return {number}
   */
  get firstVisibleIndex() {
    return this.__adapter.adjustedFirstVisibleIndex;
  }

  /**
   * Gets the index of the last visible item in the viewport.
   *
   * @return {number}
   */
  get lastVisibleIndex() {
    return this.__adapter.adjustedLastVisibleIndex;
  }

  /**
   * The size of the virtualizer
   * @return {number | undefined} The size of the virtualizer
   */
  get size() {
    return this.__adapter.size;
  }

  /**
   * The size of the virtualizer
   * @param {number} size The size of the virtualizer
   */
  set size(size) {
    this.__adapter.size = size;
  }

  /**
   * Scroll to a specific index in the virtual list
   *
   * @method scrollToIndex
   * @param {number} index The index of the item
   */
  scrollToIndex(index) {
    this.__adapter.scrollToIndex(index);
  }

  /**
   * Requests the virtualizer to re-render the item elements on an index range, if currently in the DOM
   *
   * @method update
   * @param {number | undefined} startIndex The start index of the range
   * @param {number | undefined} endIndex The end index of the range
   */
  update(startIndex = 0, endIndex = this.size - 1) {
    this.__adapter.update(startIndex, endIndex);
  }

  /**
   * Flushes active asynchronous tasks so that the component and the DOM end up in a stable state
   *
   * @method update
   * @param {number | undefined} startIndex The start index of the range
   * @param {number | undefined} endIndex The end index of the range
   */
  flush() {
    this.__adapter.flush();
  }
}
