/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { isEmptyTextNode } from './dom-utils.js';
import { SlotObserver } from './slot-observer.js';
import { generateUniqueId } from './unique-id-utils.js';

/**
 * A controller for providing content to slot element and observing changes.
 */
export class SlotController extends EventTarget {
  /**
   * Ensure that every instance has unique ID.
   *
   * @param {HTMLElement} host
   * @param {string} slotName
   * @return {string}
   * @protected
   */
  static generateId(host, prefix = 'default') {
    return `${prefix}-${host.localName}-${generateUniqueId()}`;
  }

  constructor(host, slotName, tagName, config = {}) {
    super();

    const { initializer, multiple, observe, useUniqueId, uniqueIdPrefix } = config;

    this.host = host;
    this.slotName = slotName;
    this.tagName = tagName;
    this.observe = typeof observe === 'boolean' ? observe : true;
    this.multiple = typeof multiple === 'boolean' ? multiple : false;
    this.slotInitializer = initializer;

    if (multiple) {
      this.nodes = [];
    }

    // Only generate the default ID if requested by the controller.
    if (useUniqueId) {
      this.defaultId = this.constructor.generateId(host, uniqueIdPrefix || slotName);
    }
  }

  hostConnected() {
    if (!this.initialized) {
      if (this.multiple) {
        this.initMultiple();
      } else {
        this.initSingle();
      }

      if (this.observe) {
        this.observeSlot();
      }

      this.initialized = true;
    }
  }

  /** @protected */
  initSingle() {
    let node = this.getSlotChild();

    if (!node) {
      node = this.attachDefaultNode();
      this.initNode(node);
    } else {
      this.node = node;
      this.initAddedNode(node);
    }
  }

  /** @protected */
  initMultiple() {
    const children = this.getSlotChildren();

    if (children.length === 0) {
      const defaultNode = this.attachDefaultNode();
      if (defaultNode) {
        this.nodes = [defaultNode];
        this.initNode(defaultNode);
      }
    } else {
      this.nodes = children;
      children.forEach((node) => {
        this.initAddedNode(node);
      });
    }
  }

  /**
   * Create and attach default node using the provided tag name, if any.
   * @return {Node | undefined}
   * @protected
   */
  attachDefaultNode() {
    const { host, slotName, tagName } = this;

    // Check if the node was created previously and if so, reuse it.
    let node = this.defaultNode;

    // Tag name is optional, sometimes we don't init default content.
    if (!node && tagName) {
      node = document.createElement(tagName);
      if (node instanceof Element) {
        if (slotName !== '') {
          node.setAttribute('slot', slotName);
        }
        this.defaultNode = node;
      }
    }

    if (node) {
      this.node = node;
      host.appendChild(node);
    }

    return node;
  }

  /**
   * Return the list of nodes matching the slot managed by the controller.
   * @return {Node}
   */
  getSlotChildren() {
    const { slotName } = this;
    return Array.from(this.host.childNodes).filter((node) => {
      // Either an element (any slot) or a text node (only un-named slot).
      return (
        (node.nodeType === Node.ELEMENT_NODE && node.slot === slotName) ||
        (node.nodeType === Node.TEXT_NODE && node.textContent.trim() && slotName === '')
      );
    });
  }

  /**
   * Return a reference to the node managed by the controller.
   * @return {Node}
   */
  getSlotChild() {
    return this.getSlotChildren()[0];
  }

  /**
   * Run `slotInitializer` for the node managed by the controller.
   *
   * @param {Node} node
   * @protected
   */
  initNode(node) {
    const { slotInitializer } = this;
    // Don't try to bind `this` to initializer (normally it's arrow function).
    // Instead, pass the host as a first argument to access component's state.
    if (slotInitializer) {
      slotInitializer(node, this.host);
    }
  }

  /**
   * Override to initialize the newly added custom node.
   *
   * @param {Node} _node
   * @protected
   */
  initCustomNode(_node) {}

  /**
   * Override to teardown slotted node when it's removed.
   *
   * @param {Node} _node
   * @protected
   */
  teardownNode(_node) {}

  /**
   * Run both `initCustomNode` and `initNode` for a custom slotted node.
   *
   * @param {Node} node
   * @protected
   */
  initAddedNode(node) {
    if (node !== this.defaultNode) {
      this.initCustomNode(node);
      this.initNode(node);
    }
  }

  /**
   * Setup the observer to manage slot content changes.
   * @protected
   */
  observeSlot() {
    const { slotName } = this;
    const selector = slotName === '' ? 'slot:not([name])' : `slot[name=${slotName}]`;
    const slot = this.host.shadowRoot.querySelector(selector);

    this.__slotObserver = new SlotObserver(slot, ({ addedNodes, removedNodes }) => {
      const current = this.multiple ? this.nodes : [this.node];

      // Calling `slot.assignedNodes()` includes whitespace text nodes in case of default slot:
      // unlike comment nodes, they are not filtered out. So we need to manually ignore them.
      const newNodes = addedNodes.filter((node) => !isEmptyTextNode(node) && !current.includes(node));

      if (removedNodes.length) {
        this.nodes = current.filter((node) => !removedNodes.includes(node));

        removedNodes.forEach((node) => {
          this.teardownNode(node);
        });
      }

      if (newNodes && newNodes.length > 0) {
        if (this.multiple) {
          // Remove default node if exists
          if (this.defaultNode) {
            this.defaultNode.remove();
          }
          this.nodes = [...current, ...newNodes].filter((node) => node !== this.defaultNode);
          newNodes.forEach((node) => {
            this.initAddedNode(node);
          });
        } else {
          // Remove previous node if exists
          if (this.node) {
            this.node.remove();
          }
          this.node = newNodes[0];
          this.initAddedNode(this.node);
        }
      }
    });
  }
}
