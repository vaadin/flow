/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { getFocusableElements, isElementFocused } from './focus-utils.js';

const instances = [];

/**
 * A controller for trapping focus within a DOM node.
 */
export class FocusTrapController {
  /**
   * @param {HTMLElement} host
   */
  constructor(host) {
    /**
     * The controller host element.
     *
     * @type {HTMLElement}
     */
    this.host = host;

    /**
     * A node for trapping focus in.
     *
     * @type {HTMLElement | null}
     * @private
     */
    this.__trapNode = null;

    this.__onKeyDown = this.__onKeyDown.bind(this);
  }

  /**
   * An array of tab-ordered focusable elements inside the trap node.
   *
   * @return {HTMLElement[]}
   * @private
   */
  get __focusableElements() {
    return getFocusableElements(this.__trapNode);
  }

  /**
   * The index of the element inside the trap node that currently has focus.
   *
   * @return {HTMLElement | undefined}
   * @private
   */
  get __focusedElementIndex() {
    const focusableElements = this.__focusableElements;
    return focusableElements.indexOf(focusableElements.filter(isElementFocused).pop());
  }

  hostConnected() {
    document.addEventListener('keydown', this.__onKeyDown);
  }

  hostDisconnected() {
    document.removeEventListener('keydown', this.__onKeyDown);
  }

  /**
   * Activates a focus trap for a DOM node that will prevent focus from escaping the node.
   * The trap can be deactivated with the `.releaseFocus()` method.
   *
   * If focus is initially outside the trap, the method will move focus inside,
   * on the first focusable element of the trap in the tab order.
   * The first focusable element can be the trap node itself if it is focusable
   * and comes first in the tab order.
   *
   * If there are no focusable elements, the method will throw an exception
   * and the trap will not be set.
   *
   * @param {HTMLElement} trapNode
   */
  trapFocus(trapNode) {
    this.__trapNode = trapNode;

    if (this.__focusableElements.length === 0) {
      this.__trapNode = null;
      throw new Error('The trap node should have at least one focusable descendant or be focusable itself.');
    }

    instances.push(this);

    if (this.__focusedElementIndex === -1) {
      this.__focusableElements[0].focus();
    }
  }

  /**
   * Deactivates the focus trap set with the `.trapFocus()` method
   * so that it becomes possible to tab outside the trap node.
   */
  releaseFocus() {
    this.__trapNode = null;

    instances.pop();
  }

  /**
   * A `keydown` event handler that manages tabbing navigation when the trap is enabled.
   *
   * - Moves focus to the next focusable element of the trap on `Tab` press.
   * When no next element to focus, the method moves focus to the first focusable element.
   * - Moves focus to the prev focusable element of the trap on `Shift+Tab` press.
   * When no prev element to focus, the method moves focus to the last focusable element.
   *
   * @param {KeyboardEvent} event
   * @private
   */
  __onKeyDown(event) {
    if (!this.__trapNode) {
      return;
    }

    // Only handle events for the last instance
    if (this !== Array.from(instances).pop()) {
      return;
    }

    if (event.key === 'Tab') {
      event.preventDefault();

      const backward = event.shiftKey;
      this.__focusNextElement(backward);
    }
  }

  /**
   * - Moves focus to the next focusable element if `backward === false`.
   * When no next element to focus, the method moves focus to the first focusable element.
   * - Moves focus to the prev focusable element if `backward === true`.
   * When no prev element to focus the method moves focus to the last focusable element.
   *
   * If no focusable elements, the method returns immediately.
   *
   * @param {boolean} backward
   * @private
   */
  __focusNextElement(backward = false) {
    const focusableElements = this.__focusableElements;
    const step = backward ? -1 : 1;
    const currentIndex = this.__focusedElementIndex;
    const nextIndex = (focusableElements.length + currentIndex + step) % focusableElements.length;
    const element = focusableElements[nextIndex];
    element.focus();
    if (element.localName === 'input') {
      element.select();
    }
  }
}
