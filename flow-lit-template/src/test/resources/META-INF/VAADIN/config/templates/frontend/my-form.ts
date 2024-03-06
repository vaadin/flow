/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, LitElement } from 'lit';
import { customElement } from 'lit/decorators.js';
// @customElement("my-form")
export class MyFormElement extends LitElement {
  render() {
    return html `
      <h2>Hello</h2>
      <vaadin-text-field id="nameField"></vaadin-text-field>
    `;
  }
}
customElements.define("my-form", MyFormElement);
