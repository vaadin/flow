import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class HiddenTemplate extends PolymerElement {
  static get is() {
    return 'hidden-template'
  }

  static get template() {
    return html`
       <div id='hidden-child' hidden>Foo</div>
       <div id='child'>Bar</div>
        <button on-click="updateVisibility" id='visibility'>Update Visibility</button>
    `;
  }

}
customElements.define(HiddenTemplate.is, HiddenTemplate);
