/**
 * @license
 * Copyright (c) 2022 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { isElementFocused, isElementHidden } from './focus-utils.js';
import { KeyboardMixin } from './keyboard-mixin.js';

/**
 * A mixin for navigating items with keyboard.
 *
 * @polymerMixin
 * @mixes KeyboardMixin
 */
export const KeyboardDirectionMixin = (superclass) =>
  class KeyboardDirectionMixinClass extends KeyboardMixin(superclass) {
    /**
     * @return {Element | null}
     * @protected
     */
    get focused() {
      return (this._getItems() || []).find(isElementFocused);
    }

    /**
     * @return {boolean}
     * @protected
     */
    get _vertical() {
      return true;
    }

    /**
     * @return {boolean}
     * @protected
     */
    get _tabNavigation() {
      return false;
    }

    /** @protected */
    focus() {
      const items = this._getItems();
      if (Array.isArray(items)) {
        const idx = this._getAvailableIndex(items, 0, null, (item) => !isElementHidden(item));
        if (idx >= 0) {
          this._focus(idx);
        }
      }
    }

    /**
     * Get the list of items participating in keyboard navigation.
     * By default, it treats all the light DOM children as items.
     * Override this method to provide custom list of elements.
     *
     * @return {Element[]}
     * @protected
     */
    _getItems() {
      return Array.from(this.children);
    }

    /**
     * Override an event listener from `KeyboardMixin`.
     *
     * @param {!KeyboardEvent} event
     * @protected
     * @override
     */
    _onKeyDown(event) {
      super._onKeyDown(event);

      if (event.metaKey || event.ctrlKey) {
        return;
      }

      const { key, shiftKey } = event;
      const items = this._getItems() || [];
      const currentIdx = items.indexOf(this.focused);

      let idx;
      let increment;

      const isRTL = !this._vertical && this.getAttribute('dir') === 'rtl';
      const dirIncrement = isRTL ? -1 : 1;

      if (this.__isPrevKeyPressed(key, shiftKey)) {
        increment = -dirIncrement;
        idx = currentIdx - dirIncrement;
      } else if (this.__isNextKeyPressed(key, shiftKey)) {
        increment = dirIncrement;
        idx = currentIdx + dirIncrement;
      } else if (key === 'Home') {
        increment = 1;
        idx = 0;
      } else if (key === 'End') {
        increment = -1;
        idx = items.length - 1;
      }

      idx = this._getAvailableIndex(items, idx, increment, (item) => !isElementHidden(item));

      if (
        this._tabNavigation &&
        key === 'Tab' &&
        ((idx > currentIdx && event.shiftKey) || (idx < currentIdx && !event.shiftKey) || idx === currentIdx)
      ) {
        // Prevent "roving tabindex" logic and let the normal Tab behavior if
        // - currently on the first focusable item and Shift + Tab is pressed,
        // - currently on the last focusable item and Tab is pressed,
        // - currently on the only focusable item and Tab is pressed
        return;
      }

      if (idx >= 0) {
        event.preventDefault();
        this._focus(idx, true);
      }
    }

    /**
     * @param {string} key
     * @param {boolean} shiftKey
     * @return {boolean}
     * @private
     */
    __isPrevKeyPressed(key, shiftKey) {
      if (this._vertical) {
        return key === 'ArrowUp';
      }

      return key === 'ArrowLeft' || (this._tabNavigation && key === 'Tab' && shiftKey);
    }

    /**
     * @param {string} key
     * @param {boolean} shiftKey
     * @return {boolean}
     * @private
     */
    __isNextKeyPressed(key, shiftKey) {
      if (this._vertical) {
        return key === 'ArrowDown';
      }

      return key === 'ArrowRight' || (this._tabNavigation && key === 'Tab' && !shiftKey);
    }

    /**
     * Focus the item at given index. Override this method to add custom logic.
     *
     * @param {number} index
     * @param {boolean} navigating
     * @protected
     */
    _focus(index, navigating = false) {
      const items = this._getItems();

      this._focusItem(items[index], navigating);
    }

    /**
     * Focus the given item. Override this method to add custom logic.
     *
     * @param {Element} item
     * @param {boolean} navigating
     * @protected
     */
    _focusItem(item) {
      if (item) {
        item.focus();

        // Generally, the items are expected to implement `FocusMixin`
        // that would set this attribute based on the `keydown` event.
        // We set it manually to handle programmatic focus() calls.
        item.setAttribute('focus-ring', '');
      }
    }

    /**
     * Returns index of the next item that satisfies the given condition,
     * based on the index of the current item and a numeric increment.
     *
     * @param {Element[]} items - array of items to iterate over
     * @param {number} index - index of the current item
     * @param {number} increment - numeric increment, can be either 1 or -1
     * @param {Function} condition - function used to check the item
     * @return {number}
     * @protected
     */
    _getAvailableIndex(items, index, increment, condition) {
      const totalItems = items.length;
      let idx = index;
      for (let i = 0; typeof idx === 'number' && i < totalItems; i += 1, idx += increment || 1) {
        if (idx < 0) {
          idx = totalItems - 1;
        } else if (idx >= totalItems) {
          idx = 0;
        }

        const item = items[idx];

        if (this._isItemFocusable(item) && this.__isMatchingItem(item, condition)) {
          return idx;
        }
      }
      return -1;
    }

    /**
     * Returns true if the item matches condition.
     *
     * @param {Element} item - item to check
     * @param {Function} condition - function used to check the item
     * @return {number}
     * @private
     */
    __isMatchingItem(item, condition) {
      return typeof condition === 'function' ? condition(item) : true;
    }

    /**
     * Returns whether the item is focusable. By default,
     * returns true if the item is not disabled.
     *
     * @param {Element} item
     * @return {boolean}
     * @protected
     */
    _isItemFocusable(item) {
      return !item.hasAttribute('disabled');
    }
  };
