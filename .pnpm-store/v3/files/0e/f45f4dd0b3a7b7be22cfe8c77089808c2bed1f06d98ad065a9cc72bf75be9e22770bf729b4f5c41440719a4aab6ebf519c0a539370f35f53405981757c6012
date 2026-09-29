/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { addListener } from '@vaadin/component-base/src/gestures.js';
import { DisabledMixin } from './disabled-mixin.js';
import { KeyboardMixin } from './keyboard-mixin.js';

/**
 * A mixin to toggle the `active` attribute.
 *
 * The attribute is set whenever the user activates the element by a pointer
 * or presses an activation key on the element from the keyboard.
 *
 * The attribute is removed as soon as the element is deactivated
 * by the pointer or by releasing the activation key.
 *
 * @polymerMixin
 * @mixes DisabledMixin
 * @mixes KeyboardMixin
 */
export const ActiveMixin = (superclass) =>
  class ActiveMixinClass extends DisabledMixin(KeyboardMixin(superclass)) {
    /**
     * An array of activation keys.
     *
     * See possible values here:
     * https://developer.mozilla.org/ru/docs/Web/API/KeyboardEvent/key/Key_Values
     *
     * @protected
     * @return {!Array<!string>}
     */
    get _activeKeys() {
      return [' '];
    }

    /** @protected */
    ready() {
      super.ready();

      addListener(this, 'down', (event) => {
        if (this._shouldSetActive(event)) {
          this._setActive(true);
        }
      });

      addListener(this, 'up', () => {
        this._setActive(false);
      });
    }

    /** @protected */
    disconnectedCallback() {
      super.disconnectedCallback();

      // When the element is disconnecting from the DOM at the moment being active,
      // the `active` attribute needs to be manually removed from the element.
      // Otherwise, it will preserve on the element until the element is activated once again.
      // The case reproduces for `<vaadin-date-picker>` when closing on `Cancel` or `Today` click.
      this._setActive(false);
    }

    /**
     * @param {KeyboardEvent | MouseEvent} _event
     * @protected
     */
    _shouldSetActive(_event) {
      return !this.disabled;
    }

    /**
     * Sets the `active` attribute on the element if an activation key is pressed.
     *
     * @param {KeyboardEvent} event
     * @protected
     * @override
     */
    _onKeyDown(event) {
      super._onKeyDown(event);

      if (this._shouldSetActive(event) && this._activeKeys.includes(event.key)) {
        this._setActive(true);

        // Element can become hidden before the `keyup` event, e.g. on button click.
        // Use document listener to ensure `active` attribute is removed correctly.
        document.addEventListener(
          'keyup',
          (e) => {
            if (this._activeKeys.includes(e.key)) {
              this._setActive(false);
            }
          },
          { once: true },
        );
      }
    }

    /**
     * Toggles the `active` attribute on the element.
     *
     * @param {boolean} active
     * @protected
     */
    _setActive(active) {
      this.toggleAttribute('active', active);
    }
  };
