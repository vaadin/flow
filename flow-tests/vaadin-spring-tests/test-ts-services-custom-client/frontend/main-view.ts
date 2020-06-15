import { css, customElement, html, LitElement, property } from 'lit-element';

import * as appEndpoint from './generated/AppEndpoint';

@customElement('main-view')
export class MainView extends LitElement {

  @property()
  private content = "";

  render() {
    return html`
      <button id="helloAnonymous" @click="${this.helloAnonymous}">endpoint helloAnonymous</button><br/>
      <div id="content">${this.content}</div>
    `;
  }

  async helloAnonymous() {
    try {
      this.content = await appEndpoint.helloAnonymous();
    } catch (error) {
      this.content = 'Error:' + error;
    }
  }

  static get styles() {
    return [
      css`
        :host {
          display: block;
          height: 100%;
        }
      `,
    ];
  }
}
