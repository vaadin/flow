import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class MaterialThemedTemplate extends PolymerElement {
  static get is() { return 'material-themed-template' }

  static get template() {
    return html`
       <div id='div'>Template</div>
    `;
  }
}
customElements.define(MaterialThemedTemplate.is, MaterialThemedTemplate);
