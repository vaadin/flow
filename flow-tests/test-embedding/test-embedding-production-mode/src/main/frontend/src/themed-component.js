/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';
import {ThemableMixin} from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';

class ThemedComponent extends ThemableMixin(PolymerElement) {
  static get template() {
    return html`
        <div part="content">Just a div</div>
    `;
  }

  static get is() { return 'themed-component' }
}

customElements.define(ThemedComponent.is, ThemedComponent);
