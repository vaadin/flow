import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ExistingDomElement extends PolymerElement {
  static get template() {
    return html`
       <div>
            <input id="input" on-change="valueChange">
            <div>
                <label id="label"></label>
            </div>
        </div>
        <button on-click="clear" id="button">Clear</button>
    `;
  }
  static get is() {
    return 'existing-dom-element'
  }
    
}
  
customElements.define(ExistingDomElement.is, ExistingDomElement);
