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


class AttributeTemplate extends PolymerElement {
  static get template() {
    return html`
       <div style="padding: 10px; border: 1px solid black">
       <div id="div" title="foo" foo="bar" baz></div>
       <div id="disabled" disabled></div>
       <div id="hasText">foo</div>
       <div id="hasTextAndChild">foo <label>bar</label> baz</div>
       </div>
       <slot>
    `;
  }
  static get is() {
    return 'attribute-template'
  }
    
}
  
customElements.define(AttributeTemplate.is, AttributeTemplate);
