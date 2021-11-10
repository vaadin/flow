import { html, LitElement } from 'lit';

class ReflectivelyReferencedComponent extends LitElement {
  render() {
    return html`<span>ReflectivelyReferencedComponent contents</span>`;
  }
}

customElements.define('reflectively-referenced-component', ReflectivelyReferencedComponent);
