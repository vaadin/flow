/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, LitElement } from 'lit';

class AddonLitComponent extends LitElement {
  render() {
    return html`<div>
        <p>Add-on component</p>
        <span id="label">Default</span>
    </div>`;
  }
}

customElements.define('addon-lit-component', AddonLitComponent);
