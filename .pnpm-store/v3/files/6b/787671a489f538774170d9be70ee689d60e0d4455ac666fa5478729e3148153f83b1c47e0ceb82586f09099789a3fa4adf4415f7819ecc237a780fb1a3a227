/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * A controller which prevents the virtual keyboard from showing up on mobile devices
 * when the field's overlay is closed.
 */
export class VirtualKeyboardController {
  /**
   * @param {{ inputElement?: HTMLElement; opened: boolean } & HTMLElement} host
   */
  constructor(host) {
    this.host = host;

    host.addEventListener('opened-changed', () => {
      if (!host.opened) {
        // Prevent opening the virtual keyboard when the input gets re-focused on dropdown close
        this.__setVirtualKeyboardEnabled(false);
      }
    });

    // Re-enable virtual keyboard on blur, so it gets opened when the field is focused again
    host.addEventListener('blur', () => this.__setVirtualKeyboardEnabled(true));

    // Re-enable the virtual keyboard whenever the field is touched
    host.addEventListener('touchstart', () => this.__setVirtualKeyboardEnabled(true));
  }

  /** @private */
  __setVirtualKeyboardEnabled(value) {
    if (this.host.inputElement) {
      this.host.inputElement.inputMode = value ? '' : 'none';
    }
  }
}
