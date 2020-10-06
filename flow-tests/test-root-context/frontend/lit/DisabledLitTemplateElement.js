import {html, LitElement} from 'lit-element';
import './lit-template-inner.js';

class DisabledLitTemplateElement extends LitElement {

  render() {
    return html`
        <div id='injected' disabled>Disabled</div>
        <slot></slot>
    `;
  }
}

customElements.define('disabled-lit-element', DisabledLitTemplateElement);