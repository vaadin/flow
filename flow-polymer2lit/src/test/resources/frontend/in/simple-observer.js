/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";

class SimpleObserver extends PolymerElement {
  static get template() {
    return html`
      <vaadin-vertical-layout>
        <span>First: [[first]] (last value: [[previousFirst]])</span>
        <span>Last: [[last]] (last value: [[previousLast]])</span>
      </vaadin-vertical-layout>
    `;
  }

  static get properties() {
    return {
      first: { type: String, observer: "_firstChanged" },
      last: { type: String, observer: "_lastChanged" },
      previousFirst: String,
      previousLast: String,
      example: { type: String, observer: "userListChanged(users.*, filter)" },
    };
  }

  _firstChanged(newFirst, oldFirst) {
    this.previousFirst = oldFirst;
  }

  _lastChanged(newLast, oldLast) {
    this.previousLast = oldLast;
  }

  static get is() {
    return "simple-observer";
  }
}

customElements.define(SimpleObserver.is, SimpleObserver);
