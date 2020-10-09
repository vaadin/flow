import { LitElement, html, css, customElement } from 'lit-element';

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
