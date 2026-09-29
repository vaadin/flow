/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';
import { DelegateStateMixin } from '@vaadin/component-base/src/delegate-state-mixin.js';
import { InputMixin } from './input-mixin.js';
import { ValidateMixin } from './validate-mixin.js';

/**
 * A mixin to combine multiple input validation constraints.
 *
 * @polymerMixin
 * @mixes DelegateStateMixin
 * @mixes InputMixin
 * @mixes ValidateMixin
 */
export const InputConstraintsMixin = dedupingMixin(
  (superclass) =>
    class InputConstraintsMixinClass extends DelegateStateMixin(ValidateMixin(InputMixin(superclass))) {
      /**
       * An array of attributes which participate in the input validation.
       * Changing these attributes will cause the input to re-validate.
       *
       * IMPORTANT: The attributes should be properly delegated to the input element
       * from the host using `delegateAttrs` getter (see `DelegateStateMixin`).
       * The `required` attribute is already delegated.
       */
      static get constraints() {
        return ['required'];
      }

      static get delegateAttrs() {
        return [...super.delegateAttrs, 'required'];
      }

      /** @protected */
      ready() {
        super.ready();

        this._createConstraintsObserver();
      }

      /**
       * Returns true if the current input value satisfies all constraints (if any).
       * @return {boolean}
       */
      checkValidity() {
        if (this.inputElement && this._hasValidConstraints(this.constructor.constraints.map((c) => this[c]))) {
          return this.inputElement.checkValidity();
        }
        return !this.invalid;
      }

      /**
       * Returns true if some of the provided set of constraints are valid.
       * @param {Array} constraints
       * @return {boolean}
       * @protected
       */
      _hasValidConstraints(constraints) {
        return constraints.some((c) => this.__isValidConstraint(c));
      }

      /**
       * Override this method to customize setting up constraints observer.
       * @protected
       */
      _createConstraintsObserver() {
        // This complex observer needs to be added dynamically instead of using `static get observers()`
        // to make it possible to tweak this behavior in classes that apply this mixin.
        this._createMethodObserver(`_constraintsChanged(stateTarget, ${this.constructor.constraints.join(', ')})`);
      }

      /**
       * Override this method to implement custom validation constraints.
       * @param {HTMLElement | undefined} stateTarget
       * @param {unknown[]} constraints
       * @protected
       */
      _constraintsChanged(stateTarget, ...constraints) {
        // The input element's validity cannot be determined until
        // all the necessary constraint attributes aren't set on it.
        if (!stateTarget) {
          return;
        }

        const hasConstraints = this._hasValidConstraints(constraints);
        const isLastConstraintRemoved = this.__previousHasConstraints && !hasConstraints;

        if ((this._hasValue || this.invalid) && hasConstraints) {
          this.validate();
        } else if (isLastConstraintRemoved) {
          this._setInvalid(false);
        }

        this.__previousHasConstraints = hasConstraints;
      }

      /**
       * Override an event listener inherited from `InputMixin`
       * to capture native `change` event and make sure that
       * a new one is dispatched after validation runs.
       * @param {Event} event
       * @protected
       * @override
       */
      _onChange(event) {
        event.stopPropagation();

        this.validate();

        this.dispatchEvent(
          new CustomEvent('change', {
            detail: {
              sourceEvent: event,
            },
            bubbles: event.bubbles,
            cancelable: event.cancelable,
          }),
        );
      }

      /** @private */
      __isValidConstraint(constraint) {
        // 0 is valid for `minlength` and `maxlength`
        return Boolean(constraint) || constraint === 0;
      }
    },
);
