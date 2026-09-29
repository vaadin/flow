/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';

/**
 * A mixin to provide disabled property for field components.
 *
 * @polymerMixin
 */
export const DisabledMixin = dedupingMixin(
  (superclass) =>
    class DisabledMixinClass extends superclass {
      static get properties() {
        return {
          /**
           * If true, the user cannot interact with this element.
           */
          disabled: {
            type: Boolean,
            value: false,
            observer: '_disabledChanged',
            reflectToAttribute: true,
          },
        };
      }

      /**
       * @param {boolean} disabled
       * @protected
       */
      _disabledChanged(disabled) {
        this._setAriaDisabled(disabled);
      }

      /**
       * @param {boolean} disabled
       * @protected
       */
      _setAriaDisabled(disabled) {
        if (disabled) {
          this.setAttribute('aria-disabled', 'true');
        } else {
          this.removeAttribute('aria-disabled');
        }
      }

      /**
       * Overrides the default element `click` method in order to prevent
       * firing the `click` event when the element is disabled.
       * @protected
       * @override
       */
      click() {
        if (!this.disabled) {
          super.click();
        }
      }
    },
);
