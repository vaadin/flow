/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-if.js';

class MyTemplate extends PolymerElement {
  static get is() { return 'my-one-way-template' }
    
  static get template() {
    return html`
        <div id="messageDiv">[[message]]</div>
        <div id="titleDiv">[[title]]</div>
        <button on-click="changeModelValue" id="changeModelValue">Change model value</button>

        <template is="dom-if" if="[[!title]]">
            <div id="titleDivConditional"></div>
        </template>
        <template is="dom-if" if="[[!nonExistingProperty]]">
            <div id="nonExistingProperty"></div>
        </template>
    `;
  }
}
customElements.define(MyTemplate.is, MyTemplate);
