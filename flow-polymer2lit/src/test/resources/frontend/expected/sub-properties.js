import { html, LitElement, css } from "lit";

class SubProperties extends LitElement {
  render() {
    return html`
      <div>${this.prop?.sub?.something}</div>
      <div>
        Method: ${this.abc(this.prop?.sub?.something, this.prop?.value)}
      </div>
      <div .foo="${this.prop?.sub?.something}">maybe with foo</div>
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
