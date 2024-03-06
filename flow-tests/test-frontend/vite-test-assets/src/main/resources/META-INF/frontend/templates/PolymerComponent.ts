/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {PolymerElement,html} from '@polymer/polymer/polymer-element.js';

class PolymerComponent extends PolymerElement {

  static get template() {
    return html`<div>
        <p>Local Polymer component</p>
        <span id="label">Default</span>
    </div>`;
  }

  static get is() {
    return 'polymer-component';
  }
}

customElements.define(PolymerComponent.is, PolymerComponent);
