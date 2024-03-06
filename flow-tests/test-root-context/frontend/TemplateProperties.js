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

class TemplateProperties extends PolymerElement {
  static get is() { return 'template-properties' }
    
  static get template() {
    return html`
       <div id="name" >[[name]]</div>
        <button on-click='handleClick' id="set-property">Set name property</button>
    `;
  }
}
customElements.define(TemplateProperties.is, TemplateProperties);

