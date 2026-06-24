/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { LitElement, html } from 'lit';
import { customElement } from 'lit/decorators.js';
import { Greetings } from 'Frontend/views/another.js'

@customElement('lit-view')
export class LitView extends LitElement {

  render() {
      const greetings = new Greetings();
      const hello = greetings.sayHello("John Doe");

      return html`
          <div><p>Greetings from test web component: ${hello}</p></div>
      `;
  }
}
