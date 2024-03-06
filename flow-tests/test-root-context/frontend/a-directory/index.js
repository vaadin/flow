/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { LitElement, html } from 'lit';

export class ADirectoryComponent extends LitElement {
  render() {
    return html`Directory import ok`;
  }
}
customElements.define('a-directory-component', ADirectoryComponent);
