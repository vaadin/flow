/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class SetInitialText extends PolymerElement {
  static get template() {
    return html`
       <div id='child'></div>
       <button id='addClientSideChild' on-click="_addClientSideChild">Add client side child</button>
       <slot></slot>
    `;
   }
    
   _addClientSideChild() {
     let element = document.createElement("div");
     element.innerHTML = "Client child";
     element.id='client-side';
     this.$.child.appendChild(element);
  }
  static get is() {
    return 'set-initial-text'
  }
    
}
  
customElements.define(SetInitialText.is, SetInitialText);