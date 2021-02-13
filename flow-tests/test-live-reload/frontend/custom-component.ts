import { html, LitElement } from 'lit-element';
import { customElement } from 'lit-element/decorators';

@customElement('custom-component')
export class CustomComponent extends LitElement {
  render() {
    return html`
      <div id="custom-div">Custom component contents</div>
    `;
  }
}
