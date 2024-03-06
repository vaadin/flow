/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class MyFormElement extends PolymerElement {
  static get template() {
    return html`
        <h2>Hello</h2>
        <vaadin-text-field id="nameField"></vaadin-text-field>`;
  }

  static get is() {
    return 'my-form';
  }

}

customElements.define(MyFormElement.is, MyFormElement);
