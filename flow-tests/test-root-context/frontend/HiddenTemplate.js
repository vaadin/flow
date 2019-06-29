import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class HiddenTemplate extends PolymerElement {
  static get is() {
    return 'hidden-template'
  }

  static get template() {
    return html`
       <div id='child' hidden>Foo</div>
        <button on-click="updateVisibility" id='visibility'>Update Visibility</button>
        <button on-click="updateHidden" id="hidden">Update Hidden Attribute</button>
    `;
  }

  updateHidden(){
      this.$['child'].hidden=!this.$['child'].hidden;
  }
}
customElements.define(HiddenTemplate.is, HiddenTemplate);
