/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
export class AboutView extends HTMLElement {
  connectedCallback() {
    this.innerHTML = `<h1>About Page</h1>`
  }
}

customElements.define('about-view', AboutView);
