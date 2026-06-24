/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { html, LitElement, css } from "lit";

class SubProperties extends LitElement {
  render() {
    return html`
      <div>
        ${this.prop && this.prop.sub ? this.prop.sub.something : undefined}
      </div>
      <div>
        Method:
        ${this.abc(
          this.prop && this.prop.sub ? this.prop.sub.something : undefined,
          this.prop ? this.prop.value : undefined
        )}
      </div>
      <div
        .foo="${this.prop && this.prop.sub
          ? this.prop.sub.something
          : undefined}"
      >
        maybe with foo
      </div>
    `;
  }

  and(a, b) {
    return a && b;
  }

  static get is() {
    return "sub-properties";
  }
}

customElements.define(SubProperties.is, SubProperties);
