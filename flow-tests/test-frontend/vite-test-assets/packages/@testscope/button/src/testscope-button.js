/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
export class Button extends HTMLElement {
  static get is() {
    return 'testscope-button';
  }

  connectedCallback() {
    if (!this.textContent) {
      this.textContent = 'testscope-button NOT from bundle';
    }
  }

  get isFromBundle() {
    return false;
  }
}

customElements.define('testscope-button', Button);
