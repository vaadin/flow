import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";

class EventHandlers extends PolymerElement {
  static get template() {
    return html`
      <vaadin-vertical-layout class="form-cont">
        <vaadin-text-field
          id="name"
          label="Name"
          required
          on-change="formUpdated"
          error-message="Please enter your name here"
          value="{{name}}"
        ></vaadin-text-field>
        <div class="row">
          <vaadin-combo-box
            id="statuses"
            label="Status"
            items="[[availableStatuses]]"
            value="{{status}}"
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
