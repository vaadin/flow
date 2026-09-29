/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { FieldAriaController } from '@vaadin/a11y-base/src/field-aria-controller.js';
import { ControllerMixin } from '@vaadin/component-base/src/controller-mixin.js';
import { ErrorController } from './error-controller.js';
import { HelperController } from './helper-controller.js';
import { LabelMixin } from './label-mixin.js';
import { ValidateMixin } from './validate-mixin.js';

/**
 * A mixin to provide common field logic: label, error message and helper text.
 *
 * @polymerMixin
 * @mixes ControllerMixin
 * @mixes LabelMixin
 * @mixes ValidateMixin
 */
export const FieldMixin = (superclass) =>
  class FieldMixinClass extends ValidateMixin(LabelMixin(ControllerMixin(superclass))) {
    static get properties() {
      return {
        /**
         * A target element to which ARIA attributes are set.
         * @protected
         */
        ariaTarget: {
          type: Object,
          observer: '_ariaTargetChanged',
        },

        /**
         * Error to show when the field is invalid.
         *
         * @attr {string} error-message
         */
        errorMessage: {
          type: String,
          observer: '_errorMessageChanged',
        },

        /**
         * String used for the helper text.
         * @attr {string} helper-text
         */
        helperText: {
          type: String,
          observer: '_helperTextChanged',
        },

        /**
         * String used to label the component to screen reader users.
         * @attr {string} accessible-name
         */
        accessibleName: {
          type: String,
          observer: '_accessibleNameChanged',
        },

        /**
         * Id of the element used as label of the component to screen reader users.
         * @attr {string} accessible-name-ref
         */
        accessibleNameRef: {
          type: String,
          observer: '_accessibleNameRefChanged',
        },
      };
    }

    static get observers() {
      return ['_invalidChanged(invalid)', '_requiredChanged(required)'];
    }

    constructor() {
      super();

      this._fieldAriaController = new FieldAriaController(this);
      this._helperController = new HelperController(this);
      this._errorController = new ErrorController(this);

      this._errorController.addEventListener('slot-content-changed', (event) => {
        this.toggleAttribute('has-error-message', event.detail.hasContent);
      });

      this._labelController.addEventListener('slot-content-changed', (event) => {
        const { hasContent, node } = event.detail;
        this.__labelChanged(hasContent, node);
      });

      this._helperController.addEventListener('slot-content-changed', (event) => {
        const { hasContent, node } = event.detail;
        this.toggleAttribute('has-helper', hasContent);
        this.__helperChanged(hasContent, node);
      });
    }

    /**
     * @protected
     * @return {HTMLElement}
     */
    get _errorNode() {
      return this._errorController.node;
    }

    /**
     * @protected
     * @return {HTMLElement}
     */
    get _helperNode() {
      return this._helperController.node;
    }

    /** @protected */
    ready() {
      super.ready();

      this.addController(this._fieldAriaController);
      this.addController(this._helperController);
      this.addController(this._errorController);
    }

    /** @private */
    __helperChanged(hasHelper, helperNode) {
      if (hasHelper) {
        this._fieldAriaController.setHelperId(helperNode.id);
      } else {
        this._fieldAriaController.setHelperId(null);
      }
    }

    /** @protected */
    _accessibleNameChanged(accessibleName) {
      this._fieldAriaController.setAriaLabel(accessibleName);
    }

    /** @protected */
    _accessibleNameRefChanged(accessibleNameRef) {
      this._fieldAriaController.setLabelId(accessibleNameRef, true);
    }

    /** @private */
    __labelChanged(hasLabel, labelNode) {
      // Label ID should be only added when the label content is present.
      // Otherwise, it may conflict with an `aria-label` attribute possibly added by the user.
      if (hasLabel) {
        this._fieldAriaController.setLabelId(labelNode.id);
      } else {
        this._fieldAriaController.setLabelId(null);
      }
    }

    /**
     * @param {string | null | undefined} errorMessage
     * @protected
     */
    _errorMessageChanged(errorMessage) {
      this._errorController.setErrorMessage(errorMessage);
    }

    /**
     * @param {string} helperText
     * @protected
     */
    _helperTextChanged(helperText) {
      this._helperController.setHelperText(helperText);
    }

    /**
     * @param {HTMLElement | null | undefined} target
     * @protected
     */
    _ariaTargetChanged(target) {
      if (target) {
        this._fieldAriaController.setTarget(target);
      }
    }

    /**
     * @param {boolean} required
     * @protected
     */
    _requiredChanged(required) {
      this._fieldAriaController.setRequired(required);
    }

    /**
     * @param {boolean} invalid
     * @protected
     */
    _invalidChanged(invalid) {
      this._errorController.setInvalid(invalid);

      // This timeout is needed to prevent NVDA from announcing the error message twice:
      // 1. Once adding the `[role=alert]` attribute when updating `has-error-message` (OK).
      // 2. Once linking the error ID with the ARIA target here (unwanted).
      // Related issue: https://github.com/vaadin/web-components/issues/3061.
      setTimeout(() => {
        // Error message ID needs to be dynamically added / removed based on the validity
        // Otherwise assistive technologies would announce the error, even if we hide it.
        if (invalid) {
          const node = this._errorNode;
          this._fieldAriaController.setErrorId(node && node.id);
        } else {
          this._fieldAriaController.setErrorId(null);
        }
      });
    }
  };
