/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { LitElement, html } from 'lit';

export class InFrontend extends LitElement {
  render() {
    return html`<span>This is the component from META-INF/frontend</span>`;
  }
}

customElements.define('in-frontend', InFrontend);
