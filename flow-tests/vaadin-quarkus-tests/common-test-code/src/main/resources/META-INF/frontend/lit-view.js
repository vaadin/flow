import { LitElement, html } from 'lit';

class LitView extends LitElement {
  render() {
    return html`<span id="text"></span>`;
  }
}
customElements.define('lit-view', LitView);
