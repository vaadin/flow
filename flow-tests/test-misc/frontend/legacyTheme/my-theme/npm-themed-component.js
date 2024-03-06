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
import './client-side-component.js';

class NpmThemedComponent extends PolymerElement {
  static get template() {
    return html`
       <div id="themed">Themed Component</div>
       <client-side-component></client-side-component>
`;
  }
  
  static get is() {
      return 'npm-themed-component'
  }
  
}
  
customElements.define(NpmThemedComponent.is, NpmThemedComponent);
