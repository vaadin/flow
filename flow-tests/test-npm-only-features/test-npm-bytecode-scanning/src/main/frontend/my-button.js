import { LitElement, html } from 'lit';

class MyButton extends LitElement {
  render() {
    return html`<button>Click me</button>`;
  }
}

customElements.define('my-button', MyButton);