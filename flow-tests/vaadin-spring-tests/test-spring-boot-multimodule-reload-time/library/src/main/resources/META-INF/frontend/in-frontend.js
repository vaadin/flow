import { LitElement, html } from 'lit';

export class InFrontend extends LitElement {
  render() {
    return html`<span>This is the component from META-INF/frontend</span>`;
  }
}

customElements.define('in-frontend', InFrontend);
