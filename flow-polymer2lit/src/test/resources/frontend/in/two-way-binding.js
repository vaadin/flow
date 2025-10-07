import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';

class TwoWayBinding extends PolymerElement {
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
          <vaadin-combo-box id="statuses" label="Status" items="[[availableStatuses]]" value="{{status}}">
          </vaadin-combo-box>
        </div>
        <div>Name: {{name}}</div>
        <div>Status: {{status}}</div>
      </vaadin-vertical-layout>
    `;
  }

  formUpdated() {
    console.log('Form updated');
  }

  static get is() {
    return 'two-way-binding';
  }
}

customElements.define(TwoWayBinding.is, TwoWayBinding);
