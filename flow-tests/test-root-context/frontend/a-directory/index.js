import { LitElement, html } from 'lit';

export class ADirectoryComponent extends LitElement {
  render() {
    return html`Directory import ok`;
  }
}
customElements.define('a-directory-component', ADirectoryComponent);
