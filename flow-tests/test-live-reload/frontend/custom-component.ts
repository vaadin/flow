import { html, LitElement } from 'lit';
import { customElement } from 'lit/decorators';

@customElement('custom-component')
export class CustomComponent extends LitElement {
  render() {
    return html`
      <div id="custom-div">Custom component contents</div>
    `;
  }
}
