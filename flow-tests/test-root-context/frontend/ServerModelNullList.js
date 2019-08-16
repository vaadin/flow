import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ServerModelNullList extends PolymerElement {
  static get is() { return 'server-model-null-list'; }

  static get template() {
    return html`
         This custom element has a corresponding server model with List property that is never changed.
    `;
  }
}

window.customElements.define(ServerModelNullList.is, ServerModelNullList);
