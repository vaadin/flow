/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

import { LitElement, html } from "lit";

export class SetInitialTextLit extends LitElement {
  render() {
    return html`
      <div id='child'></div>
       <button id="addClientSideChild" @click="${e => this._addClientSideChild()}">Add client side child</button>
       <slot></slot>
    `;
  }
  _addClientSideChild() {
     let element = document.createElement("div");
     element.innerHTML = "Client child";
     element.id='client-side';
     this.shadowRoot.querySelector('#child').appendChild(element);
  }
}
  
customElements.define("set-initial-text-lit", SetInitialTextLit);
