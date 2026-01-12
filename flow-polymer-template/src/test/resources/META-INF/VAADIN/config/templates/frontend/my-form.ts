import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class MyFormElement extends PolymerElement {
  static get template() {
    return html` <h2>Hello</h2>
      <vaadin-text-field id="nameField"></vaadin-text-field>`;
  }

  static get is() {
    return 'my-form';
  }
}

customElements.define(MyFormElement.is, MyFormElement);
