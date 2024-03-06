/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/button/src/vaadin-button.js';

class SimpleViewElement extends PolymerElement {
  static get template() {
    return html`
        <vaadin-button id="button">Click</vaadin-button>
        <vaadin-text-field id="log"></vaadin-text-field>
    `;
  }

  static get is() {
    return 'simple-view'
  }
}
customElements.define(SimpleViewElement.is, SimpleViewElement);
