import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ChildOrderTemplate extends PolymerElement {
  static get is() { return 'child-order-template' }

  static get template() {
    return html`
       <div id="containerWithElement">
        <div>Client child</div>
    </div>
    <div>
        <button id="addClientSideChildToContainer1" on-click="_addClientSideChildToContainer1">Add client-side child to container 1</button>
        <button id="addChildToContainer1">Add child to container 1</button>
        <button id="prependChildToContainer1">Prepend child to container 1</button>
        <button id="removeChildFromContainer1">Remove last child of container 1</button>
    </div>

    <div id="containerWithText">
        Client text
    </div>
    <div>
        <button id="addClientSideChildToContainer2" on-click="_addClientSideChildToContainer2">Add client-side child to container 2</button>
        <button id="addChildToContainer2">Add child to container 2</button>
        <button id="prependChildToContainer2">Prepend child to container 2</button>
        <button id="removeChildFromContainer2">Remove last child of container 2</button>
    </div>

    <div id="containerWithElementAddedOnConstructor">
        <div>Client child</div>
    </div>
    `;
  }

  _addClientSideChildToContainer1() {
    let element = document.createElement("div");
    element.innerHTML = "Client child";
    this.$.containerWithElement.appendChild(element);
  }

  _addClientSideChildToContainer2() {
    let text = document.createTextNode("\nClient text");
    this.$.containerWithText.appendChild(text);
  }
}
customElements.define(ChildOrderTemplate.is, ChildOrderTemplate);
