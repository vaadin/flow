/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";
import '@polymer/polymer/lib/elements/dom-if.js';
import "@vaadin/vaadin-vertical-layout";

class DomIfTest extends PolymerElement {
  static get template() {
    return html`
      <vaadin-vertical-layout>
        <template is="dom-if" if="{{showDetails}}">
          <span>Here are some details you asked for [[name]]</span>
          <span>Here are more details you asked for [[name]]</span>
        </template>
        <dom-if if="{{showDetails}}">
          <template>
            <span>Here some more details, [[name]]</span>
            <span>Here even more details, [[name]]</span>
          </template>
        </dom-if>
      </vaadin-vertical-layout>
    `;
  }

  static get is() {
    return "dom-if-test";
  }
}

customElements.define(DomIfTest.is, DomIfTest);
