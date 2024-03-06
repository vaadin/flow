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
import '@polymer/polymer/lib/elements/dom-repeat.js';


class BasicTypeList extends PolymerElement {
  static get template() {
    return html`
        <template is="dom-repeat" items="{{items}}">
            <div class="item">[[item]]</div>
        </template>
    `;
  }
  static get is() {
    return 'basic-type-list'
  }
    
}
  
customElements.define(BasicTypeList.is, BasicTypeList);
