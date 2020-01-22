import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';

class Router extends PolymerElement {
  static get is() {
    return 'router-link'
  }

  static get template() {
    return html`
        <input id="input">
        <a router-link href="com.vaadin.flow.uitest.ui.template.RouterLinksTemplate" id="navigate-link">Navigate</a>
    `;
  }
}
customElements.define(Router.is, Router);
