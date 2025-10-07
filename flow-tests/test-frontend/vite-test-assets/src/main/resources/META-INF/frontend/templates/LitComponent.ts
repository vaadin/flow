import { html, LitElement } from 'lit';

class LitComponent extends LitElement {
  render() {
    return html`<div>
      <p>Local Lit component</p>
      <span id="label">Default</span>
    </div>`;
  }
}

customElements.define('lit-component', LitComponent);
