import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";

class SubProperties extends PolymerElement {
  static get template() {
    return html`
      <div>[[prop.sub.something]]</div>
      <div>Method: [[abc(prop.sub.something, prop.value)]]</div>
      <div foo="[[prop.sub.something]]">maybe with foo</div>
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
