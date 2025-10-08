import { html, LitElement, css } from 'lit';

class DisabledUsingMethod extends LitElement {
  render() {
    return html`
      <vaadin-vertical-layout id="buttons">
        <vaadin-button
          id="signUp"
          theme="primary"
          .disabled="${!this.and(this.property1, this.property2)}"
          @click="${this.submit}"
          >Sign Up</vaadin-button
        >
        <vaadin-button id="cancelSignUpBtn" theme="tertiary" @click="${this.cancelButtonClicked}">Cancel</vaadin-button>
      </vaadin-vertical-layout>

      <span class="payment-notes">Month-to-month @ $500 / month</span>
      <a class="support" .href="${this.contactLink}" @href-changed="${(e) => (this.contactLink = e.target.value)}"
        >Contact Support</a
      >
    `;
  }

  and(a, b) {
    return a && b;
  }

  static get is() {
    return 'disabled-using-method';
  }

  static get properties() {
    return {
      property1: {
        type: Boolean
      },
      property2: {
        type: Boolean
      }
    };
  }
  constructor() {
    super();
    this.property1 = false;
    this.property2 = true;
  }
}

customElements.define(DisabledUsingMethod.is, DisabledUsingMethod);
