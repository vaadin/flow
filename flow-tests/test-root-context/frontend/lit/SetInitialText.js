
import { LitElement, html } from "lit-element";

export class SetInitialTextLit extends LitElement {
  render() {
    return html`
      <div id='child'></div>
       <button id="addClientSideChild" on-click="_addClientSideChild">Add client side child</button>
       <slot></slot>
    `;
  }
  _addClientSideChild() {
     let element = document.createElement("div");
     element.innerHTML = "Client child";
     element.id='client-side';
     this.$.child.appendChild(element);
  }
}
  
customElements.define("set-initial-text-lit", SetInitialTextLit);
