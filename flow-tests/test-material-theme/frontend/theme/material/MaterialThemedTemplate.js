import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class MaterialThemedTemplate extends PolymerElement {
  static get is() { return 'material-themed-template' }

  static get template() {
    return html`
       <div id='div'>Material themed Template</div>
       <style>
       div {
        color: var(--material-error-color); /* color */
        font-size: var(--material-font-size-xxxl); /* typography */
       }
       </style>
    `;
  }
}
customElements.define(MaterialThemedTemplate.is, MaterialThemedTemplate);
