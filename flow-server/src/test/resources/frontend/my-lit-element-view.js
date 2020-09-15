
import { LitElement, html } from "lit-element";

export class SimpleLitTemplateShadowRoot extends LitElement {
  static get properties() {
    return {
      text: String
    };
  }
  render() {
    return html`
      <button id="clientButton" @click="${e => this.$server.sayHello()}">${this.text}</button>

      <button id="mappedButton"></button>
      <div id="label"></div>
    `;
  }
}
customElements.define("my-lit-element-view", SimpleLitTemplateShadowRoot);
