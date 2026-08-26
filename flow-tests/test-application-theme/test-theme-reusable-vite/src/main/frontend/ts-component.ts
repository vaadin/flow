import { css, html, LitElement } from 'lit';
import { customElement } from 'lit/decorators.js';
import { applyTheme } from './generated/theme';

@customElement('ts-component')
export class TsComponent extends LitElement {
  connectedCallback() {
    super.connectedCallback();
    applyTheme(this.renderRoot);
  }
  static get styles() {
    return css`
      [theme='badge'] {
        background-color: rgba(51, 139, 255, 0.13);
      }
    `;
  }
  render() {
    return html` <div theme="badge">This is a badge</div> `;
  }
}
