import { LitElement, html } from 'lit';

export class InjectedTemplate extends LitElement {
  static get properties() {
    return {
      value: { type: Number }
    };
  }
  render() {
    return html` <div>Injected Template</div> `;
  }
}
customElements.define('injected-lit-template', InjectedTemplate);
