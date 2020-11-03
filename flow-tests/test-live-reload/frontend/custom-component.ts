import { customElement, html, LitElement } from 'lit-element';

@customElement('custom-component')
export class CustomComponent extends LitElement {
  render() {
    return html`
      <div id="custom-div">Custom component contents</div>
    `;
  }
}
