import { html, LitElement } from 'lit';

class LitComponent extends LitElement {
  render() {
    return html`<span id="label">Default</span>`;
  }
}

customElements.define('lit-component', LitComponent);
