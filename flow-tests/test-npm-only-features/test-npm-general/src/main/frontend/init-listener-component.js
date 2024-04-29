import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class InitListenerComponent extends PolymerElement {
  static get template() {
    return html`
        <div>Init Listener Component</div>
    `;
  }

  static get is() {
    return 'init-listener-component'
  }
}
customElements.define(InitListenerComponent.is, InitListenerComponent);
