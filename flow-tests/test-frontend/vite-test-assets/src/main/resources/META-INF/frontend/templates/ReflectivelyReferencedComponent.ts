/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, LitElement } from 'lit';

class ReflectivelyReferencedComponent extends LitElement {
  render() {
    return html`<span>ReflectivelyReferencedComponent contents</span>`;
  }
}

customElements.define('reflectively-referenced-component', ReflectivelyReferencedComponent);
