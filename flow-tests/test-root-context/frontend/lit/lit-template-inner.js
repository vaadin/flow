import {html, LitElement} from 'lit-element';

class TemplateInner extends LitElement {

  render() {
    return html`
        <div>Hello template inner</div>
    `;
  }
}

customElements.define('lit-template-inner', TemplateInner);