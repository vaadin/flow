import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";

class DisabledUsingMethod extends PolymerElement {
  static get template() {
    return html`
      <vaadin-vertical-layout id="buttons">
        <vaadin-button
          id="signUp"
          theme="primary"
          disabled="[[!and(property1, property2)]]"
          on-click="submit"
          >Sign Up</vaadin-button
        >
        <vaadin-button
          id="cancelSignUpBtn"
          theme="tertiary"
          on-click="cancelButtonClicked"
          >Cancel</vaadin-button
        >
      </vaadin-vertical-layout>

      <span class="payment-notes">Month-to-month @ $500 / month</span>
      <a class="support" href="{{contactLink}}">Contact Support</a>
    `;
  }

  and(a, b) {
    return a && b;
  }

  static get is() {
    return "disabled-using-method";
  }

  static get properties() {
    return {
      property1: {
        type: Boolean,
        value: false,
      },
      property2: {
        type: Boolean,
        value: true,
      },
    };
  }
}

customElements.define(DisabledUsingMethod.is, DisabledUsingMethod);
