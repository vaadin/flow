import { LitElement, html, css, customElement } from 'lit-element';

@customElement('another-view')
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
      <div id="another-content">Another</div>
    `;
  }
}
