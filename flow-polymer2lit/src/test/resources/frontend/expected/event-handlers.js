import { html, LitElement, css } from "lit";

class EventHandlers extends LitElement {
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
      </vaadin-vertical-layout>
    `;
  }

  static get is() {
    return "event-handlers";
  }

  submit() {
    console.log("Submit clicked");
  }

  formUpdated() {
    console.log("Form updated");
  }
}

customElements.define(EventHandlers.is, EventHandlers);
