/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { DisabledMixin } from './disabled-mixin.js';

/**
 * A mixin to toggle the `tabindex` attribute.
 *
 * The attribute is set to -1 whenever the user disables the element
 * and restored with the last known value once the element is enabled.
 *
 * @polymerMixin
 * @mixes DisabledMixin
 */
export const TabindexMixin = (superclass) =>
  class TabindexMixinClass extends DisabledMixin(superclass) {
    static get properties() {
      return {
        /**
         * Indicates whether the element can be focused and where it participates in sequential keyboard navigation.
         *
         * @protected
         */
        tabindex: {
          type: Number,
          reflectToAttribute: true,
          observer: '_tabindexChanged',
          sync: true,
        },

        /**
         * Stores the last known tabindex since the element has been disabled.
         *
         * @protected
         */
        _lastTabIndex: {
          type: Number,
        },
      };
    }

    /**
     * When the element gets disabled, the observer saves the last known tabindex
     * and makes the element not focusable by setting tabindex to -1.
     * As soon as the element gets enabled, the observer restores the last known tabindex
     * so that the element can be focusable again.
     *
     * @protected
     * @override
     */
    _disabledChanged(disabled, oldDisabled) {
      super._disabledChanged(disabled, oldDisabled);

      if (this.__shouldAllowFocusWhenDisabled()) {
        return;
      }

      if (disabled) {
        if (this.tabindex !== undefined) {
          this._lastTabIndex = this.tabindex;
        }
        this.setAttribute('tabindex', '-1');
      } else if (oldDisabled) {
        if (this._lastTabIndex !== undefined) {
          this.setAttribute('tabindex', this._lastTabIndex);
        } else {
          this.tabindex = undefined;
        }
      }
    }

    /**
     * When the user has changed tabindex while the element is disabled,
     * the observer reverts tabindex to -1 and rather saves the new tabindex value to apply it later.
     * The new value will be applied as soon as the element becomes enabled.
     *
     * @protected
     */
    _tabindexChanged(tabindex) {
      if (this.__shouldAllowFocusWhenDisabled()) {
        return;
      }

      if (this.disabled && tabindex !== -1) {
        this._lastTabIndex = tabindex;
        this.setAttribute('tabindex', '-1');
      }
    }

    /**
     * Overrides the native `focus` method in order to prevent
     * focusing the element when it is disabled. Note, setting
     * `tabindex` to -1 does not prevent the element from being
     * programmatically focusable.
     *
     * @protected
     * @override
     */
    focus() {
      if (!this.disabled || this.__shouldAllowFocusWhenDisabled()) {
        super.focus();
      }
    }

    /**
     * Returns whether the component should be focusable when disabled.
     * Returns false by default.
     *
     * @private
     * @return {boolean}
     */
    __shouldAllowFocusWhenDisabled() {
      return false;
    }
  };
