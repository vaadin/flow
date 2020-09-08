import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';


class AttributeTemplate extends PolymerElement {
  static get template() {
    return html`
       <div style="padding: 10px; border: 1px solid black">
       <div id="div" title="foo"></div>
       </div>
       <slot>
    `;
  }
  static get is() {
    return 'attribute-template'
  }
    
}
  
customElements.define(AttributeTemplate.is, AttributeTemplate);
