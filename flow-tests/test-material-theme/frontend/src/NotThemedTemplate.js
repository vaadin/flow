import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class NotThemedTemplate extends PolymerElement {
  static get is() { return 'not-themed-template' }

  static get template() {
    return html`
       <div id='div'>Template</div>
    `;
  }
}
customElements.define(NotThemedTemplate.is, NotThemedTemplate);
