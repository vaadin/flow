import {PolymerElement,html} from '@polymer/polymer/polymer-element.js';

class PolymerComponent extends PolymerElement {

  static get template() {
    return html`<span id="label">Default</span>`;
  }
  static get is() {
    return 'polymer-component';
  }
}

customElements.define(PolymerComponent.is, PolymerComponent);
