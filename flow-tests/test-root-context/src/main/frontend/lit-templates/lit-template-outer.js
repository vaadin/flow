import { html, LitElement } from 'lit';
import './lit-template-inner.js';

class TemplateOuter extends LitElement {
  render() {
    return html`
      <div>Hello template outer</div>
      <lit-template-inner id="inner" style="display: block"></lit-template-inner>
    `;
  }
}

customElements.define('lit-template-outer', TemplateOuter);
