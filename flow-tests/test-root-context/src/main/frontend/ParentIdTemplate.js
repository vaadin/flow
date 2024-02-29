import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ParentIdTemplate extends PolymerElement {
  static get is() { return 'parent-id-template' }

  static get template() {
    return html`
       <style>
        :host {
            width: 100%;
            padding: 10px;
        }
    </style>
    <div>Parent Template</div>

    <child-id-template id="child"></child-id-template>
    `;
  }
}
customElements.define(ParentIdTemplate.is, ParentIdTemplate);
