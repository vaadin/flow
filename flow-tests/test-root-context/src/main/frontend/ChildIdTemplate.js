import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ChildIdTemplate extends PolymerElement {
  static get is() { return 'child-id-template' }

  static get template() {
    return html`
       <div style="padding: 10px; border: 1px solid black">
        <div>Child template</div>
        <div id="text"></div>
    </div>
    `;
  }
}
customElements.define(ChildIdTemplate.is, ChildIdTemplate);
