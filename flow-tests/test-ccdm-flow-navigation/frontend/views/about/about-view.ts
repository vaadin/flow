import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators';
import './about-view.global.css';

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
      <div>hello view</div>
      <a href="hello" tabindex="-1" id="navigate-hello">Hello Flow</a>
    `;
  }
}
