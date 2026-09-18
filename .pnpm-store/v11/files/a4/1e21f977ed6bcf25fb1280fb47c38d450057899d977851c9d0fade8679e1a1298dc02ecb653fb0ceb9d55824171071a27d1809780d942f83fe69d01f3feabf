/**
 * @license
 * Copyright (c) 2017 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { isEmptyTextNode } from '@vaadin/component-base/src/dom-utils.js';
import { SlotObserver } from '@vaadin/component-base/src/slot-observer.js';

/**
 * @polymerMixin
 */
export const HorizontalLayoutMixin = (superClass) =>
  class extends superClass {
    /** @protected */
    ready() {
      super.ready();

      const startSlot = this.shadowRoot.querySelector('slot:not([name])');
      this.__startSlotObserver = new SlotObserver(startSlot, ({ currentNodes, removedNodes }) => {
        if (removedNodes.length) {
          this.__clearAttribute(removedNodes, 'last-start-child');
        }

        const children = currentNodes.filter((node) => node.nodeType === Node.ELEMENT_NODE);
        this.__updateAttributes(children, 'start', false, true);

        const nodes = currentNodes.filter((node) => !isEmptyTextNode(node));
        this.toggleAttribute('has-start', nodes.length > 0);
      });

      const endSlot = this.shadowRoot.querySelector('[name="end"]');
      this.__endSlotObserver = new SlotObserver(endSlot, ({ currentNodes, removedNodes }) => {
        if (removedNodes.length) {
          this.__clearAttribute(removedNodes, 'first-end-child');
        }

        this.__updateAttributes(currentNodes, 'end', true, false);

        this.toggleAttribute('has-end', currentNodes.length > 0);
      });

      const middleSlot = this.shadowRoot.querySelector('[name="middle"]');
      this.__middleSlotObserver = new SlotObserver(middleSlot, ({ currentNodes, removedNodes }) => {
        if (removedNodes.length) {
          this.__clearAttribute(removedNodes, 'first-middle-child');
          this.__clearAttribute(removedNodes, 'last-middle-child');
        }

        this.__updateAttributes(currentNodes, 'middle', true, true);

        this.toggleAttribute('has-middle', currentNodes.length > 0);
      });
    }

    /** @private */
    __clearAttribute(nodes, attr) {
      const el = nodes.find((node) => node.nodeType === Node.ELEMENT_NODE && node.hasAttribute(attr));
      if (el) {
        el.removeAttribute(attr);
      }
    }

    /** @private */
    __updateAttributes(nodes, slot, setFirst, setLast) {
      nodes.forEach((child, idx) => {
        if (setFirst) {
          const attr = `first-${slot}-child`;
          if (idx === 0) {
            child.setAttribute(attr, '');
          } else if (child.hasAttribute(attr)) {
            child.removeAttribute(attr);
          }
        }

        if (setLast) {
          const attr = `last-${slot}-child`;
          if (idx === nodes.length - 1) {
            child.setAttribute(attr, '');
          } else if (child.hasAttribute(attr)) {
            child.removeAttribute(attr);
          }
        }
      });
    }
  };
