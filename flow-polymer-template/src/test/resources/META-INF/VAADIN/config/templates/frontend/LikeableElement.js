/*
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
// Import an element
import '@polymer/paper-checkbox/paper-checkbox.js';

// Import the PolymerElement base class and html helper
import { PolymerElement, html } from '@polymer/polymer';

// Define an element class
class LikeableElement extends PolymerElement {
  // Define public API properties
  static get properties() {
    return { liked: Boolean };
  }

  // Define the element's template
  static get template() {
    return html`
      <style>
        :host {
          margin: 5px;
        }

        .response {
          margin-top: 10px;
        }
      </style>
      <div>Tag name doesn't match the JS module name</div>
      <paper-checkbox checked="{{liked}}">I like web components!</paper-checkbox>

      <div id="test" hidden$="[[!liked]]" class="response">Web components like you, too.</div>
    `;
  }
}

// Register the element with the browser
customElements.define('likeable-element', LikeableElement);
