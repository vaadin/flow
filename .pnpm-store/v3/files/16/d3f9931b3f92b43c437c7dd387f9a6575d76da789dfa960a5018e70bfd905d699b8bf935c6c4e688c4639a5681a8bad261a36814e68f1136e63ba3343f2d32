/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { hideOthers } from './aria-hidden.js';

/**
 * A controller for handling modal state on the elements with `dialog` and `alertdialog` role.
 * See https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Attributes/aria-modal
 *
 * Note, the actual `role` and `aria-modal` attributes are supposed to be handled by the
 * consumer web component. This is done in to ensure the controller only does one thing.
 */
export class AriaModalController {
  /**
   * @param {HTMLElement} host
   */
  constructor(host, callback) {
    /**
     * The controller host element.
     *
     * @type {HTMLElement}
     */
    this.host = host;

    /**
     * The callback used to detect which element
     * to use as a target. Defaults to the host.
     *
     * @type {Function}
     */
    this.callback = typeof callback === 'function' ? callback : () => host;
  }

  /**
   * Make the controller host modal by hiding other elements from screen readers
   * using `aria-hidden` attribute (can be replaced with `inert` in the future).
   *
   * The method name is chosen to align with the one provided by native `<dialog>`:
   * https://developer.mozilla.org/en-US/docs/Web/API/HTMLDialogElement/showModal
   */
  showModal() {
    const targets = this.callback();
    this.__showOthers = hideOthers(targets);
  }

  /**
   * Remove `aria-hidden` from other elements unless there are any other
   * controller hosts on the page activated by using `showModal()` call.
   */
  close() {
    if (this.__showOthers) {
      this.__showOthers();
      this.__showOthers = null;
    }
  }
}
