import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class TestComponent extends PolymerElement {
  static get template() {
    return html`
        <button id="button">Click</button>
        <button id="connect" on-click="connect">Click</button>
        <div id="content"></div>
    `;
  }

  static get is() {
    return 'test-component'
  }

  connect(e) {
    var service = 'connect/ConnectServices/hello';
    var data = {name: 'Friend'};
    
    fetch(service, {
      method: 'POST',
      body: JSON.stringify(data),
      headers:{
        'Content-Type': 'application/json'
      }
    })
    .then(res => res.json())
    .catch(error => this.$.content.textContent = 'Error:' + error)
    .then(response => this.$.content.textContent = response);
  }
}
customElements.define(TestComponent.is, TestComponent);
