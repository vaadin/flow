import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class SimpleViewElement extends PolymerElement {
  static get template() {
    return html`
      <button id="button">Click</button>
      <input id="log" />
    `;
  }

  static get is() {
    return 'simple-view';
  }
}
customElements.define(SimpleViewElement.is, SimpleViewElement);
