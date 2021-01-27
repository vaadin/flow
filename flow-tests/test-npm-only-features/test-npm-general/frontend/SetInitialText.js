/*
 * Copyright 2000-2021 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
