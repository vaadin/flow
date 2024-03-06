/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

import { LitElement, html } from "lit";

export class AttributeLitTemplate extends LitElement {
  render() {
    return html`
       <div style="padding: 10px; border: 1px solid black">
         <div id="div" title="foo" foo="bar" baz></div>
       </div>
       <slot></slot>
    `;
  }
}
customElements.define("attribute-lit-template", AttributeLitTemplate);
