import { LitElement, html } from 'lit';
import './InjectedTemplate.js';

export class InjectingTemplate extends LitElement {
  render() {
    return html`
      <div id="container"></div>
      <injected-lit-template value="1.0" id="injected"></injected-lit-template>
      <button id="show-type" @click="${(e) => this._addClientSideChild()}">Show type</button>
    `;
  }

  _addClientSideChild() {
    let element = document.createElement('div');
    element.innerHTML = typeof this.shadowRoot.querySelector('#injected').value;
    element.id = 'type-info';
    this.shadowRoot.querySelector('#container').appendChild(element);
  }
}
customElements.define('injecting-lit-template', InjectingTemplate);
