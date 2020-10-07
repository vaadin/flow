import {html, LitElement} from 'lit-element';

class DisabledLitTemplateElement extends LitElement {

  render() {
    return html`
        <div id='injected' disabled>Disabled</div>
        <slot></slot>
    `;
  }
}

customElements.define('disabled-lit-element', DisabledLitTemplateElement);