/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { InputControlMixin } from './input-control-mixin.js';

/**
 * A mixin to provide logic for vaadin-text-field and related components.
 *
 * @polymerMixin
 * @mixes InputControlMixin
 */
export const InputFieldMixin = (superclass) =>
  class InputFieldMixinClass extends InputControlMixin(superclass) {
    static get properties() {
      return {
        /**
         * Whether the value of the control can be automatically completed by the browser.
         * List of available options at:
         * https://developer.mozilla.org/en/docs/Web/HTML/Element/input#attr-autocomplete
         */
        autocomplete: {
          type: String,
        },

        /**
         * This is a property supported by Safari that is used to control whether
         * autocorrection should be enabled when the user is entering/editing the text.
         * Possible values are:
         * on: Enable autocorrection.
         * off: Disable autocorrection.
         */
        autocorrect: {
          type: String,
          reflectToAttribute: true,
        },

        /**
         * This is a property supported by Safari and Chrome that is used to control whether
         * autocapitalization should be enabled when the user is entering/editing the text.
         * Possible values are:
         * characters: Characters capitalization.
         * words: Words capitalization.
         * sentences: Sentences capitalization.
         * none: No capitalization.
         */
        autocapitalize: {
          type: String,
          reflectToAttribute: true,
        },
      };
    }

    static get delegateAttrs() {
      return [...super.delegateAttrs, 'autocapitalize', 'autocomplete', 'autocorrect'];
    }

    // Workaround for https://github.com/Polymer/polymer/issues/5259
    get __data() {
      return this.__dataValue || {};
    }

    set __data(value) {
      this.__dataValue = value;
    }

    /**
     * @param {HTMLElement} input
     * @protected
     * @override
     */
    _inputElementChanged(input) {
      super._inputElementChanged(input);

      if (input) {
        // Discard value set on the custom slotted input.
        if (input.value && input.value !== this.value) {
          console.warn(`Please define value on the <${this.localName}> component!`);
          input.value = '';
        }

        if (this.value) {
          input.value = this.value;
        }
      }
    }

    /**
     * Override an event listener from `FocusMixin`.
     * @param {boolean} focused
     * @protected
     * @override
     */
    _setFocused(focused) {
      super._setFocused(focused);

      // Do not validate when focusout is caused by document
      // losing focus, which happens on browser tab switch.
      if (!focused && document.hasFocus()) {
        this._requestValidation();
      }
    }

    /**
     * Override an event listener from `InputMixin`
     * to mark as valid after user started typing.
     * @param {Event} event
     * @protected
     * @override
     */
    _onInput(event) {
      super._onInput(event);

      if (this.invalid) {
        this._requestValidation();
      }
    }

    /**
     * Override an observer from `InputMixin` to validate the field
     * when a new value is set programmatically.
     *
     * @param {string | undefined} newValue
     * @param {string | undefined} oldValue
     * @protected
     * @override
     */
    _valueChanged(newValue, oldValue) {
      super._valueChanged(newValue, oldValue);

      if (oldValue === undefined) {
        return;
      }

      if (this.invalid) {
        this._requestValidation();
      }
    }
  };
