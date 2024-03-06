/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';

class DepElement extends PolymerElement {
  static get template() {
    return html`
        <div id='main'>Imported element</div>
    `;
  }

  static get is() { return 'dep-element' }
}

customElements.define(DepElement.is, DepElement);
