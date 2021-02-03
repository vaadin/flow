import { css, customElement, html, LitElement } from 'lit-element';

@customElement('hello-world-view')
export class HelloWorldView extends LitElement {
  static get styles() {
    return css`
      :host {
        display: block;
        padding: 1em;
      }
    `;
  }

  render() {
    return html`<vaadin-text-field id="name" label="Your name"></vaadin-text-field>
      <vaadin-button id="sayHello">Say hello</vaadin-button>`;
  }
}
