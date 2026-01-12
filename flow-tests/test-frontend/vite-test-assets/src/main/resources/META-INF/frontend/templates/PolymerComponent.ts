import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class PolymerComponent extends PolymerElement {
  static get template() {
    return html`<div>
      <p>Local Polymer component</p>
      <span id="label">Default</span>
    </div>`;
  }

  static get is() {
    return 'polymer-component';
  }
}

customElements.define(PolymerComponent.is, PolymerComponent);
