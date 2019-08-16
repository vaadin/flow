import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class InitOrderPolymer extends PolymerElement {
  static get template() {
    return html`
       Init order with Polymer
         <p id="status"></p>
    `;
  }
  static get is() {
    return 'init-order-polymer'
  }
  
  ready() {
    super.ready();

    var message = createStatusMessage(this);
    this.$.status.textContent = message;
  }
    
}

function createStatusMessage(element) {
  return  "property = " + element.property +
    ", attribute = " + element.getAttribute("attribute") +
    ", child count = " + element.childElementCount +
    ", style = " + element.style.animationName +
    ", class = " + element.getAttribute("class");
}    

customElements.define(InitOrderPolymer.is, InitOrderPolymer);
        
        
class InitOrderNopolymer extends HTMLElement {
  static get is() { return 'init-order-nopolymer' }

  constructor() {
    super();

    var shadow = this.attachShadow({mode: 'open'});
    shadow.textContent = "Init order without Polymer";

    this.status = document.createElement("p");
    this.status.id = "status";
    shadow.appendChild(this.status);
  }

  connectedCallback() {
    var message = createStatusMessage(this);

    this.status.textContent = message;
  }
}
customElements.define(InitOrderNopolymer.is, InitOrderNopolymer);
