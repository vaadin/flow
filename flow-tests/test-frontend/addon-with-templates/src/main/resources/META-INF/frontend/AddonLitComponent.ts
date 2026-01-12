import { html, LitElement } from 'lit';

class AddonLitComponent extends LitElement {
  render() {
    return html`<div>
      <p>Add-on component</p>
      <span id="label">Default</span>
    </div>`;
  }
}

customElements.define('addon-lit-component', AddonLitComponent);
