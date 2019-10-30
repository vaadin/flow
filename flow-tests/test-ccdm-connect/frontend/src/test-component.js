import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import * as connectServices from '../generated/ConnectServices';

class TestComponent extends PolymerElement {
  static get template() {
    return html`
        <button id="button">Click</button>
        <button id="connect" on-click="connect">Click</button>
        <div id="content"></div>
        <button id="takeNull" on-click="takeNull">takeNull</button>
        <button id="giveNull" on-click="giveNull">giveNull</button>
        <button id="takeOptionalNull" on-click="takeOptionalNull">takeOptionalNull</button>
        <button id="giveOptionalNull" on-click="giveOptionalNull">giveOptionalNull</button>
    `;
  }

  static get is() {
    return 'test-component'
  }

  connect(e) {
    connectServices
      .hello('Friend')
      .then(response => this.$.content.textContent = response)
      .catch(error => this.$.content.textContent = 'Error:' + error);
  }

  async takeNull(e) {
    await connectServices.takeNull(null);
  }

  async giveNull(e) {
    await connectServices.giveNull();
  }

  async takeOptionalNull(e) {
    await connectServices.takeOptionalNull(null);
  }

  async giveOptionalNull(e) {
    await connectServices.giveOptionalNull();
  }
}
customElements.define(TestComponent.is, TestComponent);
