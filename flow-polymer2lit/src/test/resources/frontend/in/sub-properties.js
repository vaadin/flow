/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";

class SubProperties extends PolymerElement {
  static get template() {
    return html`
      <div>[[prop.sub.something]]</div>
      <div>Method: [[abc(prop.sub.something, prop.value)]]</div>
      <div foo="[[prop.sub.something]]">maybe with foo</div>
    `;
  }

  and(a, b) {
    return a && b;
  }

  static get is() {
    return "sub-properties";
  }
}

customElements.define(SubProperties.is, SubProperties);
