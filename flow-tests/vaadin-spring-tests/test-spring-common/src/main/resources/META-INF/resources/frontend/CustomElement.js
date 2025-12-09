import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class CustomElement extends PolymerElement {
  static get template() {
    return html`
      <div>
        <label id="label"></label>
      </div>
    `;
  }

  static get is() {
    return 'custom-element';
  }
}
customElements.define(CustomElement.is, CustomElement);
