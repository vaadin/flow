import { html, LitElement, css } from "lit-element";

class BasicBindings extends LitElement {
  render() {
    return html`
      <vaadin-vertical-layout id="buttons">
        <vaadin-button .id="${this.buttonId}">${this.buttonText}</vaadin-button>
        <vaadin-text-field .value="${this.textfieldValue}"></vaadin-text-field>
        <div ?hidden="${this.noshow}">Visible or not</div>
      </vaadin-vertical-layout>
    `;
  }

  static get is() {
    return "basic-bindings";
  }
}

customElements.define(BasicBindings.is, BasicBindings);
