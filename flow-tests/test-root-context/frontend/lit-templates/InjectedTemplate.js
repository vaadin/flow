/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { LitElement, html } from "lit";

export class InjectedTemplate extends LitElement {
  static get properties() {
    return {
      value: { type: Number}
    };
  }
  render() {
    return html`
       <div>Injected Template</div>
    `;
  }
}
customElements.define("injected-lit-template", InjectedTemplate);
