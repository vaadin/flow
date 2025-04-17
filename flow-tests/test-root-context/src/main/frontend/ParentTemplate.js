import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import './ChildTemplate.js';

class ParentTemplate extends PolymerElement {
  static get is() { return 'parent-template' }

  static get template() {
    return html`
       <div>Parent Template</div>
        <div>
        <div>Placeholder</div>

        <child-template id="child"></child-template>

        </div>
        <style>

            parent-template {
                width: 100%;
            }
        </style>
  `;
  }
}
customElements.define(ParentTemplate.is, ParentTemplate);
