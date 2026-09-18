/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { announce } from '@vaadin/a11y-base/src/announce.js';
import { SlotChildObserveController } from '@vaadin/component-base/src/slot-child-observe-controller.js';

/**
 * A controller that manages the error message node content.
 */
export class ErrorController extends SlotChildObserveController {
  constructor(host) {
    super(host, 'error-message', 'div');
  }

  /**
   * Set the error message element text content.
   *
   * @param {string} errorMessage
   */
  setErrorMessage(errorMessage) {
    this.errorMessage = errorMessage;

    this.updateDefaultNode(this.node);
  }

  /**
   * Set invalid state for detecting whether to show error message.
   *
   * @param {boolean} invalid
   */
  setInvalid(invalid) {
    this.invalid = invalid;

    this.updateDefaultNode(this.node);
  }

  /**
   * Override method inherited from `SlotController` to not run
   * initializer on the custom slotted node unnecessarily.
   *
   * @param {Node} node
   * @protected
   * @override
   */
  initAddedNode(node) {
    if (node !== this.defaultNode) {
      // There is no need to run `initNode`.
      this.initCustomNode(node);
    }
  }

  /**
   * Override to initialize the newly added default error message.
   *
   * @param {Node} errorNode
   * @protected
   * @override
   */
  initNode(errorNode) {
    this.updateDefaultNode(errorNode);
  }

  /**
   * Override to initialize the newly added custom error message.
   *
   * @param {Node} errorNode
   * @protected
   * @override
   */
  initCustomNode(errorNode) {
    // Save the custom error message content on the host.
    if (errorNode.textContent && !this.errorMessage) {
      this.errorMessage = errorNode.textContent.trim();
    }

    // Notify the host about custom node.
    super.initCustomNode(errorNode);
  }

  /**
   * Override method inherited from `SlotChildObserveController`
   * to restore the default error message element.
   *
   * @protected
   * @override
   */
  restoreDefaultNode() {
    this.attachDefaultNode();
  }

  /**
   * Override method inherited from `SlotChildObserveController`
   * to update the error message text and hidden state.
   *
   * Note: unlike with other controllers, this method is
   * called for both default and custom error message.
   *
   * @param {Node | undefined} node
   * @protected
   * @override
   */
  updateDefaultNode(errorNode) {
    const { errorMessage, invalid } = this;
    const hasError = Boolean(invalid && errorMessage && errorMessage.trim() !== '');

    if (errorNode) {
      errorNode.textContent = hasError ? errorMessage : '';
      errorNode.hidden = !hasError;

      if (hasError) {
        // Assertive mode ensures VoiceOver reads
        // the error message on commit with Enter.
        announce(errorMessage, { mode: 'assertive' });
      }
    }

    // Notify the host after update.
    super.updateDefaultNode(errorNode);
  }
}
