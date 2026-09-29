/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { getDeepActiveElement } from './focus-utils.js';

/**
 * A controller for saving a focused node and restoring focus to it later.
 */
export class FocusRestorationController {
  /**
   * Saves the given node as a target for restoring focus to
   * when `restoreFocus()` is called. If no node is provided,
   * the currently focused node in the DOM is saved as a target.
   *
   * @param {Node | null | undefined} focusNode
   */
  saveFocus(focusNode) {
    this.focusNode = focusNode || getDeepActiveElement();
  }

  /**
   * Restores focus to the target node that was saved previously with `saveFocus()`.
   */
  restoreFocus(options) {
    const focusNode = this.focusNode;
    if (!focusNode) {
      return;
    }

    const preventScroll = options ? options.preventScroll : false;

    if (getDeepActiveElement() === document.body) {
      // In Firefox and Safari, focusing the node synchronously
      // doesn't work as expected when the overlay is closing on outside click.
      // These browsers force focus to move to the body element and retain it
      // there until the next event loop iteration.
      setTimeout(() => focusNode.focus({ preventScroll }));
    } else {
      focusNode.focus({ preventScroll });
    }

    this.focusNode = null;
  }
}
