import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";

class MultipleBindings extends PolymerElement {
  static get template() {
    return html`
      <vaadin-vertical-layout id="buttons">
        <vaadin-button id="[[buttonId]]-1">[[buttonText1]] [[buttonText2]]</vaadin-button>
        <vaadin-button id="hello [[buttonId]]">[[buttonText1]] [[buttonText2]]</vaadin-button>
        <vaadin-text-field value="[[textfieldValue1]]-[[textfieldValue2]]"></vaadin-text-field>
        <vaadin-text-field value="[[sub.value1]]-[[sub.value2]]"></vaadin-text-field>
      </vaadin-vertical-layout>
    `;
  }

  static get is() {
    return "multiple-bindings";
  }
}

customElements.define(MultipleBindings.is, MultipleBindings);
