/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { LitElement, html } from 'lit';

class LazyComponent extends LitElement {
  render() {
    return html`<span>Lazy component</span>`;
  }
}

customElements.define('lazy-component', LazyComponent);
