import { LitElement, html } from 'lit';

class MyComponentElement extends LitElement {
  render() {
    return html`
      <button id="button">Click</button>
      <div id="content"></div>
    `;
  }
}

customElements.define('my-component', MyComponentElement);
