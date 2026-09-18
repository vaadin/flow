/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { SlotChildObserveController } from '@vaadin/component-base/src/slot-child-observe-controller.js';

/**
 * A controller to manage the label element.
 */
export class LabelController extends SlotChildObserveController {
  constructor(host) {
    super(host, 'label', 'label');
  }

  /**
   * Set label based on corresponding host property.
   *
   * @param {string} label
   */
  setLabel(label) {
    this.label = label;

    // Restore the default label, if needed.
    const labelNode = this.getSlotChild();
    if (!labelNode) {
      this.restoreDefaultNode();
    }

    // When default label is used, update it.
    if (this.node === this.defaultNode) {
      this.updateDefaultNode(this.node);
    }
  }

  /**
   * Override method inherited from `SlotChildObserveController`
   * to restore and observe the default label element.
   *
   * @protected
   * @override
   */
  restoreDefaultNode() {
    const { label } = this;

    // Restore the default label.
    if (label && label.trim() !== '') {
      const labelNode = this.attachDefaultNode();

      // Observe the default label.
      this.observeNode(labelNode);
    }
  }

  /**
   * Override method inherited from `SlotChildObserveController`
   * to update the default label element text content.
   *
   * @param {Node | undefined} node
   * @protected
   * @override
   */
  updateDefaultNode(node) {
    if (node) {
      node.textContent = this.label;
    }

    // Notify the host after update.
    super.updateDefaultNode(node);
  }

  /**
   * Override to observe the newly added custom node.
   *
   * @param {Node} node
   * @protected
   * @override
   */
  initCustomNode(node) {
    // Notify the host about adding a custom node.
    super.initCustomNode(node);

    this.observeNode(node);
  }
}
