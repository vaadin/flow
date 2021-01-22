import { css, customElement, html, LitElement } from "lit-element";
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
