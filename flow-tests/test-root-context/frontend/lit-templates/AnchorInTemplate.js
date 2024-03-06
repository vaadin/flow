/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

import { LitElement, html } from "lit";

export class AnchorInTemplate extends LitElement {
  render() {
    return html`
        <a href="" download id="anchor">Download</a>
    `;
  }
}
customElements.define("anchor-in-template", AnchorInTemplate);
