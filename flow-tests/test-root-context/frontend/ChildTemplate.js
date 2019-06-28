import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ChildTemplate extends PolymerElement {
  static get is() { return 'child-template' }

  static get template() {
    return html`
       <div on-click="handleClick">Child Template</div>
    <div id="text">[[text]]</div>
    `;
  }
}
customElements.define(ChildTemplate.is, ChildTemplate);
