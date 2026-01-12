import { LitElement, html } from 'lit';

export class SetInitialTextLit extends LitElement {
  render() {
    return html`
      <div id="child"></div>
      <button id="addClientSideChild" @click="${(e) => this._addClientSideChild()}">Add client side child</button>
      <slot></slot>
    `;
  }
  _addClientSideChild() {
    let element = document.createElement('div');
    element.innerHTML = 'Client child';
    element.id = 'client-side';
    this.shadowRoot.querySelector('#child').appendChild(element);
  }
}

customElements.define('set-initial-text-lit', SetInitialTextLit);
