import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class AnchorInTemplate extends PolymerElement {
  static get template() {
    return html`
      <a href="" download id="anchor">Download</a>
    `;
  }
    
}
  
customElements.define("anchor-in-template", AnchorInTemplate);
