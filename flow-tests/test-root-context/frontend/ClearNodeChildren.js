import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ClearNodeChildren extends PolymerElement {
  static get is() { return 'clear-node-children' }

  static get template() {
    return html`
       <style>
        .container {
            padding: 10px;
            border: 1px solid gray;
            margin-bottom: 10px;
        }
    </style>

    <div id="containerWithElementChildren" class="container">
        <div>Client div 1</div>
        <div>Client div 2</div>
    </div>
    <div id="containerWithMixedChildren" class="container">
        Some text 1
        <div>Client div 1</div>
        Some text 2
        <div>Client div 2</div>
        Some text 3
    </div>
    <div id="containerWithClientSideChildren" class="container">
    </div>
    <div id="containerWithContainer" class="container">
        <div id="nestedContainer"></div>
    </div>
    <div id="containerWithSlottedChildren" class="container">
        <slot></slot>
    </div>

    <button id="addChildToContainer1">Add child to container 1</button>
    <button id="addTextNodeToContainer1">Add text node to container 1</button>
    <button id="clearContainer1">Clear container 1</button>
    <button id="setTextToContainer1">Set text to container 1</button>
    <button id="addChildToContainer2">Add child to container 2</button>
    <button id="clearContainer2">Clear container 2</button>
    <button id="setTextToContainer2">Set text to container 2</button>
    <button id="addClientSideChild" on-click="_addClientSideChild">Add client-side child</button>
    <button id="addChildToContainer3">Add child to container 3</button>
    <button id="clearContainer3">Clear container 3</button>
    <button id="setTextToContainer3">Set text to container 3</button>
    <button id="addChildToNestedContainer">Add child to nested container</button>
    <button id="clearContainer4">Clear container 4</button>
    <button id="setTextToContainer4">Set text to container 4</button>
    <button id="addChildToSlot">Add child to slot</button>
    <button id="clear">Clear slot</button>
    <button id="setText">Set text to slot</button>
    <div id="message" style="white-space: pre-wrap;"></div>
    `;
  }

  _addClientSideChild() {
    let element = document.createElement("div");
    element.innerHTML = "Client child";
    this.$.containerWithClientSideChildren.appendChild(element);
  }
}
customElements.define(ClearNodeChildren.is, ClearNodeChildren);
