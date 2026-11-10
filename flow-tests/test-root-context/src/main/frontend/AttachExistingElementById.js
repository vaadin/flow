import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class ExistingElement extends PolymerElement {
  static get template() {
    return html`
      <div>
        <input id="input" on-change="valueChange" />
        <div>
          <label id="label"></label>
        </div>
      </div>
      <button on-click="clear" id="button">Clear</button>
    `;
  }
  static get is() {
    return 'existing-element';
  }
}

customElements.define(ExistingElement.is, ExistingElement);
