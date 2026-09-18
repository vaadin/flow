/**
 * @license
 * Copyright (c) 2022 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { SlotController } from './slot-controller.js';

/**
 * A controller that observes slotted element mutations, especially ID attribute
 * and the text content, and fires an event to notify host element about those.
 */
export class SlotChildObserveController extends SlotController {
  constructor(host, slot, tagName, config = {}) {
    super(host, slot, tagName, { ...config, useUniqueId: true });
  }

  /**
   * Override to initialize the newly added custom node.
   *
   * @param {Node} node
   * @protected
   * @override
   */
  initCustomNode(node) {
    this.__updateNodeId(node);
    this.__notifyChange(node);
  }

  /**
   * Override to notify the controller host about removal of
   * the custom node, and to apply the default one if needed.
   *
   * @param {Node} _node
   * @protected
   * @override
   */
  teardownNode(_node) {
    const node = this.getSlotChild();

    // Custom node is added to the slot
    if (node && node !== this.defaultNode) {
      this.__notifyChange(node);
    } else {
      this.restoreDefaultNode();
      this.updateDefaultNode(this.node);
    }
  }

  /**
   * Override method inherited from `SlotMixin`
   * to set ID attribute on the default node.
   *
   * @return {Node}
   * @protected
   * @override
   */
  attachDefaultNode() {
    const node = super.attachDefaultNode();

    if (node) {
      this.__updateNodeId(node);
    }

    return node;
  }

  /**
   * Override to restore default node when a custom one is removed.
   *
   * @protected
   */
  restoreDefaultNode() {
    // To be implemented
  }

  /**
   * Override to update default node text on property change.
   *
   * @param {Node} node
   * @protected
   */
  updateDefaultNode(node) {
    this.__notifyChange(node);
  }

  /**
   * Setup the mutation observer on the node to update ID and notify host.
   * Node doesn't get observed automatically until this method is called.
   *
   * @param {Node} node
   * @protected
   */
  observeNode(node) {
    // Stop observing the previous node, if any.
    if (this.__nodeObserver) {
      this.__nodeObserver.disconnect();
    }

    this.__nodeObserver = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        const target = mutation.target;

        // Ensure the mutation target is the currently connected node
        // to ignore async mutations dispatched for removed element.
        const isCurrentNodeMutation = target === this.node;

        if (mutation.type === 'attributes') {
          // We use attributeFilter to only observe ID mutation,
          // no need to check for attribute name separately.
          if (isCurrentNodeMutation) {
            this.__updateNodeId(target);
          }
        } else if (isCurrentNodeMutation || target.parentElement === this.node) {
          // Node text content has changed.
          this.__notifyChange(this.node);
        }
      });
    });

    // Observe changes to node ID attribute, text content and children.
    this.__nodeObserver.observe(node, {
      attributes: true,
      attributeFilter: ['id'],
      childList: true,
      subtree: true,
      characterData: true,
    });
  }

  /**
   * Returns true if a node is an HTML element with children,
   * or is a defined custom element, or has non-empty text.
   *
   * @param {Node} node
   * @return {boolean}
   * @private
   */
  __hasContent(node) {
    if (!node) {
      return false;
    }

    return (
      (node.nodeType === Node.ELEMENT_NODE && (customElements.get(node.localName) || node.children.length > 0)) ||
      (node.textContent && node.textContent.trim() !== '')
    );
  }

  /**
   * Fire an event to notify the controller host about node changes.
   *
   * @param {Node} node
   * @private
   */
  __notifyChange(node) {
    this.dispatchEvent(
      new CustomEvent('slot-content-changed', {
        detail: { hasContent: this.__hasContent(node), node },
      }),
    );
  }

  /**
   * Set default ID on the node in case it is an HTML element.
   *
   * @param {Node} node
   * @private
   */
  __updateNodeId(node) {
    // When in multiple mode, only set ID attribute on the element in default slot.
    const isFirstNode = !this.nodes || node === this.nodes[0];
    if (node.nodeType === Node.ELEMENT_NODE && (!this.multiple || isFirstNode) && !node.id) {
      node.id = this.defaultId;
    }
  }
}
