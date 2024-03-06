/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, LitElement } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('custom-component')
export class CustomComponent extends LitElement {
  render() {
    return html`
      <div id="custom-div">Custom component contents</div>
    `;
  }
}
