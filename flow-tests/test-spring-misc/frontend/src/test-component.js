import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { ElementMixin } from '@vaadin/vaadin-element-mixin/vaadin-element-mixin.js';

class TestComponent extends ElementMixin(PolymerElement) {
  static get template() {
    return html`
        <button id="button">Click</button>
        <div id="content"></div>
    `;
  }

  static get is() {
    return 'test-component'
  }
}
customElements.define(TestComponent.is, TestComponent);
