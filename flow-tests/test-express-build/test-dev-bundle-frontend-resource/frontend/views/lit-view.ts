import { LitElement, html } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('lit-view')
export class LitView extends LitElement {

  render() {
      return html`
          <div><p>This is test web component</p></div>
      `;
  }
}
