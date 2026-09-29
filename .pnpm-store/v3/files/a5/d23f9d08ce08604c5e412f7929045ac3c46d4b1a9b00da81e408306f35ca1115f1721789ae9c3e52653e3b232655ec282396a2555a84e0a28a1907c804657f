/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';
import { isKeyboardActive } from './focus-utils.js';

/**
 * A mixin to handle `focused` and `focus-ring` attributes based on focus.
 *
 * @polymerMixin
 */
export const FocusMixin = dedupingMixin(
  (superclass) =>
    class FocusMixinClass extends superclass {
      /**
       * @protected
       * @return {boolean}
       */
      get _keyboardActive() {
        return isKeyboardActive();
      }

      /** @protected */
      ready() {
        this.addEventListener('focusin', (e) => {
          if (this._shouldSetFocus(e)) {
            this._setFocused(true);
          }
        });

        this.addEventListener('focusout', (e) => {
          if (this._shouldRemoveFocus(e)) {
            this._setFocused(false);
          }
        });

        // In super.ready() other 'focusin' and 'focusout' listeners might be
        // added, so we call it after our own ones to ensure they execute first.
        // Issue to watch out: when incorrect, <vaadin-combo-box> refocuses the
        // input field on iOS after "Done" is pressed.
        super.ready();
      }

      /** @protected */
      disconnectedCallback() {
        super.disconnectedCallback();

        // In non-Chrome browsers, blur does not fire on the element when it is disconnected.
        // reproducible in `<vaadin-date-picker>` when closing on `Cancel` or `Today` click.
        if (this.hasAttribute('focused')) {
          this._setFocused(false);
        }
      }

      /**
       * Override to change how focused and focus-ring attributes are set.
       *
       * @param {boolean} focused
       * @protected
       */
      _setFocused(focused) {
        this.toggleAttribute('focused', focused);

        // Focus-ring is true when the element was focused from the keyboard.
        // Focus Ring [A11ycasts]: https://youtu.be/ilj2P5-5CjI
        this.toggleAttribute('focus-ring', focused && this._keyboardActive);
      }

      /**
       * Override to define if the field receives focus based on the event.
       *
       * @param {FocusEvent} _event
       * @return {boolean}
       * @protected
       */
      _shouldSetFocus(_event) {
        return true;
      }

      /**
       * Override to define if the field loses focus based on the event.
       *
       * @param {FocusEvent} _event
       * @return {boolean}
       * @protected
       */
      _shouldRemoveFocus(_event) {
        return true;
      }
    },
);
