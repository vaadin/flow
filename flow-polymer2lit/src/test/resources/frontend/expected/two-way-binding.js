import { html, LitElement, css } from "lit";

class TwoWayBinding extends LitElement {
  render() {
    return html`
      <vaadin-vertical-layout class="form-cont">
        <vaadin-text-field
          id="name"
          label="Name"
          required
          error-message="Please enter your name here"
          @change="${this.formUpdated}"
          .value="${this.name}"
          @value-changed="${(e) => (this.name = e.target.value)}"
        ></vaadin-text-field>
        <div class="row">
          <vaadin-combo-box
            id="statuses"
            label="Status"
            .items="${this.availableStatuses}"
            .value="${this.status}"
            @value-changed="${(e) => (this.status = e.target.value)}"
          >
          </vaadin-combo-box>
        </div>
        <div>Name: ${this.name}</div>
        <div>Status: ${this.status}</div>
      </vaadin-vertical-layout>
    `;
  }

  formUpdated() {
    console.log("Form updated");
  }

  static get is() {
    return "two-way-binding";
  }
}

customElements.define(TwoWayBinding.is, TwoWayBinding);
