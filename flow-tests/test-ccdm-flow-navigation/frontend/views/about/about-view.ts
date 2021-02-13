import { LitElement, html, css } from 'lit-element';
import { customElement } from 'lit-element/decorators';

@customElement('about-view')
export class AboutView extends LitElement {
  static get styles() {
    return css`
      :host {
        display: block;
        padding: var(--lumo-space-m) var(--lumo-space-l);
      }
    `;
  }

  render() {
    return html`
      <a href="hello" tabindex="-1" id="navigate-hello">Hello Flow</a>
    `;
  }
}
