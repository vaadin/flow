import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class ChildTemplate extends PolymerElement {
  static get template() {
    return html`
      <div>Child Template</div>
      <label id="info">[[foo]]</label>
      <label id="message">[[message]]</label>
    `;
  }

  static get is() {
    return 'child-template';
  }
}
customElements.define(ChildTemplate.is, ChildTemplate);
