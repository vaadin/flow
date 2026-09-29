/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';

/**
 * A mixin to store the reference to an input element
 * and add input and change event listeners to it.
 *
 * @polymerMixin
 */
export const InputMixin = dedupingMixin(
  (superclass) =>
    class InputMixinClass extends superclass {
      static get properties() {
        return {
          /**
           * A reference to the input element controlled by the mixin.
           * Any component implementing this mixin is expected to provide it
           * by using `this._setInputElement(input)` Polymer API.
           *
           * A typical case is using `InputController` that does this automatically.
           * However, the input element does not have to always be native <input>:
           * as an example, <vaadin-combo-box-light> accepts other components.
           *
           * @protected
           * @type {!HTMLElement}
           */
          inputElement: {
            type: Object,
            readOnly: true,
            observer: '_inputElementChanged',
          },

          /**
           * String used to define input type.
           * @protected
           */
          type: {
            type: String,
            readOnly: true,
          },

          /**
           * The value of the field.
           */
          value: {
            type: String,
            value: '',
            observer: '_valueChanged',
            notify: true,
            sync: true,
          },

          /**
           * Whether the input element has a non-empty value.
           *
           * @protected
           */
          _hasInputValue: {
            type: Boolean,
            value: false,
            observer: '_hasInputValueChanged',
          },
        };
      }

      constructor() {
        super();

        this._boundOnInput = this.__onInput.bind(this);
        this._boundOnChange = this._onChange.bind(this);
      }

      /**
       * Indicates whether the value is different from the default one.
       * Override if the `value` property has a type other than `string`.
       *
       * @protected
       */
      get _hasValue() {
        return this.value != null && this.value !== '';
      }

      /**
       * A property for accessing the input element's value.
       *
       * Override this getter if the property is different from the default `value` one.
       *
       * @protected
       * @return {string}
       */
      get _inputElementValueProperty() {
        return 'value';
      }

      /**
       * The input element's value.
       *
       * @protected
       * @return {string}
       */
      get _inputElementValue() {
        return this.inputElement ? this.inputElement[this._inputElementValueProperty] : undefined;
      }

      /**
       * The input element's value.
       *
       * @protected
       */
      set _inputElementValue(value) {
        if (this.inputElement) {
          this.inputElement[this._inputElementValueProperty] = value;
        }
      }

      /**
       * Clear the value of the field.
       */
      clear() {
        this._hasInputValue = false;

        this.value = '';

        // Clear the input immediately without waiting for the observer.
        // Otherwise, when using Lit, the old value would be restored.
        this._inputElementValue = '';
      }

      /**
       * Add event listeners to the input element instance.
       * Override this method to add custom listeners.
       * @param {!HTMLElement} input
       * @protected
       */
      _addInputListeners(input) {
        input.addEventListener('input', this._boundOnInput);
        input.addEventListener('change', this._boundOnChange);
      }

      /**
       * Remove event listeners from the input element instance.
       * @param {!HTMLElement} input
       * @protected
       */
      _removeInputListeners(input) {
        input.removeEventListener('input', this._boundOnInput);
        input.removeEventListener('change', this._boundOnChange);
      }

      /**
       * A method to forward the value property set on the field
       * programmatically back to the input element value.
       * Override this method to perform additional checks,
       * for example to skip this in certain conditions.
       * @param {string} value
       * @protected
       */
      _forwardInputValue(value) {
        // Value might be set before an input element is initialized.
        // This case should be handled separately by a component that
        // implements this mixin, for example in `connectedCallback`.
        if (!this.inputElement) {
          return;
        }

        this._inputElementValue = value != null ? value : '';
      }

      /**
       * @param {HTMLElement | undefined} input
       * @param {HTMLElement | undefined} oldInput
       * @protected
       */
      _inputElementChanged(input, oldInput) {
        if (input) {
          this._addInputListeners(input);
        } else if (oldInput) {
          this._removeInputListeners(oldInput);
        }
      }

      /**
       * Observer to notify about the change of private property.
       *
       * @private
       */
      _hasInputValueChanged(hasValue, oldHasValue) {
        if (hasValue || oldHasValue) {
          this.dispatchEvent(new CustomEvent('has-input-value-changed'));
        }
      }

      /**
       * An input event listener used to update `_hasInputValue` property.
       * Do not override this method.
       *
       * @param {Event} event
       * @private
       */
      __onInput(event) {
        this._setHasInputValue(event);
        this._onInput(event);
      }

      /**
       * An input event listener used to update the field value.
       *
       * @param {Event} event
       * @protected
       */
      _onInput(event) {
        // In the case a custom web component is passed as `inputElement`,
        // the actual native input element, on which the event occurred,
        // can be inside shadow trees.
        const target = event.composedPath()[0];
        // Ignore fake input events e.g. used by clear button.
        this.__userInput = event.isTrusted;
        this.value = target.value;
        this.__userInput = false;
      }

      /**
       * A change event listener.
       * Override this method with an actual implementation.
       * @param {Event} _event
       * @protected
       */
      _onChange(_event) {}

      /**
       * Toggle the has-value attribute based on the value property.
       *
       * @param {boolean} hasValue
       * @protected
       */
      _toggleHasValue(hasValue) {
        this.toggleAttribute('has-value', hasValue);
      }

      /**
       * Observer called when a value property changes.
       * @param {string | undefined} newVal
       * @param {string | undefined} oldVal
       * @protected
       */
      _valueChanged(newVal, oldVal) {
        this._toggleHasValue(this._hasValue);

        // Setting initial value to empty string, do nothing.
        if (newVal === '' && oldVal === undefined) {
          return;
        }

        // Value is set by the user, no need to sync it back to input.
        if (this.__userInput) {
          return;
        }

        // Setting a value programmatically, sync it to input element.
        this._forwardInputValue(newVal);
      }

      /**
       * Sets the `_hasInputValue` property based on the `input` event.
       *
       * @param {InputEvent} event
       * @protected
       */
      _setHasInputValue(event) {
        // In the case a custom web component is passed as `inputElement`,
        // the actual native input element, on which the event occurred,
        // can be inside shadow trees.
        const target = event.composedPath()[0];
        this._hasInputValue = target.value.length > 0;
      }
    },
);
