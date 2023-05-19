import { css, html, LitElement } from "lit";
import { customElement } from "lit/decorators.js";
import { applyTheme } from "./generated/theme";

@customElement("ts-component")
export class TsComponent extends LitElement {
  connectedCallback() {
    super.connectedCallback();
    applyTheme(this.renderRoot);
  }
  static get styles() {
    return css``;
  }
  render() {
    return html` <div theme="badge">This is a badge</div> `;
  }
}
