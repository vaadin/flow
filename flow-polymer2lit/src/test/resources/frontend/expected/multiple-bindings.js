import { html, LitElement, css } from "lit";

class MultipleBindings extends LitElement {
  render() {
    return html`
      <vaadin-vertical-layout id="buttons">
        <vaadin-button .id="${this.buttonId}"
          >${this.buttonText1} ${this.buttonText2}</vaadin-button
        >
        <vaadin-button .id="${"hello " + (this.buttonId ?? "")}"
          >${this.buttonText1} ${this.buttonText2}</vaadin-button
        >
        <vaadin-text-field
          .value="${(this.textfieldValue1 ?? "") +
          "-" +
          (this.textfieldValue2 ?? "")}"
        ></vaadin-text-field>
        <vaadin-text-field
          .value="${(this.sub?.value1 ?? "") + "-" + (this.sub?.value2 ?? "")}"
        ></vaadin-text-field>
      </vaadin-vertical-layout>
    `;
  }

  static get is() {
    return "multiple-bindings";
  }
}

customElements.define(MultipleBindings.is, MultipleBindings);
