/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, LitElement, css } from 'lit';
import { customElement } from 'lit/decorators.js';
import { applyTheme } from './generated/theme';

@customElement('component-with-theme')
export class ComponentWithTheme extends LitElement {
  static get styles() {
    return css`
      #inside {
        background: rgb(100, 100, 0);
      }
    `;
  }
  render() {
    return html` <div id="inside">Hello</div> `;
  }
  connectedCallback() {
    super.connectedCallback();
    applyTheme(this.renderRoot);
  }
}
