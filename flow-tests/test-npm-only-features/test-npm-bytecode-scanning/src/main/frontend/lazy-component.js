import { LitElement, html } from 'lit';

class LazyComponent extends LitElement {
  render() {
    return html`<span>Lazy component</span>`;
  }
}

customElements.define('lazy-component', LazyComponent);
