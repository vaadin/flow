import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class LumoThemedTemplate extends PolymerElement {
  static get is() { return 'explicit-lumo-themed-template' }

  static get template() {
    return html`
       <div id='div'>Template</div>
    `;
  }
}
customElements.define(LumoThemedTemplate.is, LumoThemedTemplate);
