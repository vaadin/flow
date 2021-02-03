import { css, customElement, html, LitElement } from 'lit-element';

@customElement('type-script-view')
export class TypeScriptView extends LitElement {
  static get styles() {
    return css`
      :host {
        display: block;
        padding: 1em;
      }
    `;
  }

  render() {
    return html`<button id="mappedButton"></button>
      <div id="label"></div>`;
  }
}
