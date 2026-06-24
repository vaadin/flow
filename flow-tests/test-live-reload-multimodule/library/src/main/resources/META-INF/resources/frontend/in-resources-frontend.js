import { LitElement, html } from 'lit';

export class InResourcesFrontend extends LitElement {
  render() {
    return html`<span>This is the component from META-INF/resources/frontend</span>`;
  }
}

customElements.define('in-resources-frontend', InResourcesFrontend);
