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

class TemplatePushViewElement extends PolymerElement {
  static get template() {
    return html`
          <div>
              Hello <label id="label"></label>
              <button id="elementTest">Element API</button>
              <button id="execJsTest">Exec JS</button>
              <button id="callFunctionTest">Element.callFunction</button>
          </div>
     `;
  }

  static get is() {
    return 'template-push-view'
  }
}
customElements.define(TemplatePushViewElement.is, TemplatePushViewElement);
