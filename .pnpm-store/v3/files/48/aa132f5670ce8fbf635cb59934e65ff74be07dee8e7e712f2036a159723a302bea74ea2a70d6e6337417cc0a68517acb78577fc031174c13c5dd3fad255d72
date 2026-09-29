/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';
import { FocusMixin } from './focus-mixin.js';
import { TabindexMixin } from './tabindex-mixin.js';

/**
 * A mixin to forward focus to an element in the light DOM.
 *
 * @polymerMixin
 * @mixes FocusMixin
 * @mixes TabindexMixin
 */
export const DelegateFocusMixin = dedupingMixin(
  (superclass) =>
    class DelegateFocusMixinClass extends FocusMixin(TabindexMixin(superclass)) {
      static get properties() {
        return {
          /**
           * Specify that this control should have input focus when the page loads.
           */
          autofocus: {
            type: Boolean,
          },

          /**
           * A reference to the focusable element controlled by the mixin.
           * It can be an input, textarea, button or any element with tabindex > -1.
           *
           * Any component implementing this mixin is expected to provide it
           * by using `this._setFocusElement(input)` Polymer API.
           *
           * Toggling `tabindex` attribute on the host element propagates its value to `focusElement`.
           *
           * @protected
           * @type {!HTMLElement}
           */
          focusElement: {
            type: Object,
            readOnly: true,
            observer: '_focusElementChanged',
          },

          /**
           * Override the property from `TabIndexMixin`
           * to ensure the `tabindex` attribute of the focus element
           * will be restored to `0` after re-enabling the element.
           *
           * @protected
           * @override
           */
          _lastTabIndex: {
            value: 0,
          },
        };
      }

      constructor() {
        super();

        this._boundOnBlur = this._onBlur.bind(this);
        this._boundOnFocus = this._onFocus.bind(this);
      }

      /** @protected */
      ready() {
        super.ready();

        if (this.autofocus && !this.disabled) {
          requestAnimationFrame(() => {
            this.focus();
            this.setAttribute('focus-ring', '');
          });
        }
      }

      /**
       * @protected
       * @override
       */
      focus() {
        if (this.focusElement && !this.disabled) {
          this.focusElement.focus();
        }
      }

      /**
       * @protected
       * @override
       */
      blur() {
        if (this.focusElement) {
          this.focusElement.blur();
        }
      }

      /**
       * @protected
       * @override
       */
      click() {
        if (this.focusElement && !this.disabled) {
          this.focusElement.click();
        }
      }

      /** @protected */
      _focusElementChanged(element, oldElement) {
        if (element) {
          element.disabled = this.disabled;
          this._addFocusListeners(element);
          this.__forwardTabIndex(this.tabindex);
        } else if (oldElement) {
          this._removeFocusListeners(oldElement);
        }
      }

      /**
       * @param {HTMLElement} element
       * @protected
       */
      _addFocusListeners(element) {
        element.addEventListener('blur', this._boundOnBlur);
        element.addEventListener('focus', this._boundOnFocus);
      }

      /**
       * @param {HTMLElement} element
       * @protected
       */
      _removeFocusListeners(element) {
        element.removeEventListener('blur', this._boundOnBlur);
        element.removeEventListener('focus', this._boundOnFocus);
      }

      /**
       * Focus event does not bubble, so we dispatch it manually
       * on the host element to support adding focus listeners
       * when the focusable element is placed in light DOM.
       * @param {FocusEvent} event
       * @protected
       */
      _onFocus(event) {
        event.stopPropagation();
        this.dispatchEvent(new Event('focus'));
      }

      /**
       * Blur event does not bubble, so we dispatch it manually
       * on the host element to support adding blur listeners
       * when the focusable element is placed in light DOM.
       * @param {FocusEvent} event
       * @protected
       */
      _onBlur(event) {
        event.stopPropagation();
        this.dispatchEvent(new Event('blur'));
      }

      /**
       * @param {FocusEvent} event
       * @return {boolean}
       * @protected
       * @override
       */
      _shouldSetFocus(event) {
        return event.target === this.focusElement;
      }

      /**
       * @param {FocusEvent} event
       * @return {boolean}
       * @protected
       * @override
       */
      _shouldRemoveFocus(event) {
        return event.target === this.focusElement;
      }

      /**
       * @param {boolean} disabled
       * @param {boolean} oldDisabled
       * @protected
       * @override
       */
      _disabledChanged(disabled, oldDisabled) {
        super._disabledChanged(disabled, oldDisabled);

        if (this.focusElement) {
          this.focusElement.disabled = disabled;
        }

        if (disabled) {
          this.blur();
        }
      }

      /**
       * Override an observer from `TabindexMixin`.
       * Do not call super to remove tabindex attribute
       * from the host after it has been forwarded.
       * @param {string} tabindex
       * @protected
       * @override
       */
      _tabindexChanged(tabindex) {
        this.__forwardTabIndex(tabindex);
      }

      /** @private */
      __forwardTabIndex(tabindex) {
        if (tabindex !== undefined && this.focusElement) {
          this.focusElement.tabIndex = tabindex;

          // Preserve tabindex="-1" on the host element
          if (tabindex !== -1) {
            this.tabindex = undefined;
          }
        }

        if (this.disabled && tabindex) {
          // If tabindex attribute was changed while component was disabled
          if (tabindex !== -1) {
            this._lastTabIndex = tabindex;
          }
          this.tabindex = undefined;
        }
      }
    },
);
