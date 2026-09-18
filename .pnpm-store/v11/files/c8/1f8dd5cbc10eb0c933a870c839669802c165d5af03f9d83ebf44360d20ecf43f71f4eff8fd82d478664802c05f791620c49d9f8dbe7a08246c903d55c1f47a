/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupeMixin } from '@open-wc/dedupe-mixin';

/**
 * A mixin to provide required state and validation logic.
 *
 * @polymerMixin
 */
export const ValidateMixin = dedupeMixin(
  (superclass) =>
    class ValidateMixinClass extends superclass {
      static get properties() {
        return {
          /**
           * Set to true when the field is invalid.
           */
          invalid: {
            type: Boolean,
            reflectToAttribute: true,
            notify: true,
            value: false,
            sync: true,
          },

          /**
           * Set to true to enable manual validation mode. This mode disables automatic
           * constraint validation, allowing you to control the validation process yourself.
           * You can still trigger constraint validation manually with the `validate()` method
           * or use `checkValidity()` to assess the component's validity without affecting
           * the invalid state. In manual validation mode, you can also manipulate
           * the `invalid` property directly through your application logic without conflicts
           * with the component's internal validation.
           *
           * @attr {boolean} manual-validation
           */
          manualValidation: {
            type: Boolean,
            value: false,
          },

          /**
           * Specifies that the user must fill in a value.
           */
          required: {
            type: Boolean,
            reflectToAttribute: true,
            sync: true,
          },
        };
      }

      /**
       * Validates the field and sets the `invalid` property based on the result.
       *
       * The method fires a `validated` event with the result of the validation.
       *
       * @return {boolean} True if the value is valid.
       */
      validate() {
        const isValid = this.checkValidity();
        this._setInvalid(!isValid);
        this.dispatchEvent(new CustomEvent('validated', { detail: { valid: isValid } }));
        return isValid;
      }

      /**
       * Returns true if the field value satisfies all constraints (if any).
       *
       * @return {boolean}
       */
      checkValidity() {
        return !this.required || !!this.value;
      }

      /**
       * @param {boolean} invalid
       * @protected
       */
      _setInvalid(invalid) {
        if (this._shouldSetInvalid(invalid)) {
          this.invalid = invalid;
        }
      }

      /**
       * Override this method to define whether the given `invalid` state should be set.
       *
       * @param {boolean} _invalid
       * @return {boolean}
       * @protected
       */
      _shouldSetInvalid(_invalid) {
        return true;
      }

      /** @protected */
      _requestValidation() {
        if (!this.manualValidation) {
          // eslint-disable-next-line no-restricted-syntax
          this.validate();
        }
      }

      /**
       * Fired whenever the field is validated.
       *
       * @event validated
       * @param {Object} detail
       * @param {boolean} detail.valid the result of the validation.
       */
    },
);
