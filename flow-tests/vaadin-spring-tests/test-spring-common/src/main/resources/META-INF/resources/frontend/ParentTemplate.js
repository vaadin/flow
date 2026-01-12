import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import './ChildTemplate';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class ParentTemplate extends PolymerElement {
  static get template() {
    return html`
      <div>Parent Template</div>

      <div id="div"></div>

      <child-template id="child"></child-template>
      <style>
        parent-template {
          width: 100%;
        }
      </style>
    `;
  }

  static get is() {
    return 'parent-template';
  }
}
customElements.define(ParentTemplate.is, ParentTemplate);
