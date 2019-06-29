import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class InjectScriptTagTemplate extends PolymerElement {
  static get is() { return 'inject-script-tag-template' }

  onButtonClick() {
    this.$server.changeValue();
  }

  static get template() {
    return html`
      <div id="value-div">[[value]]</div>
      <slot></slot>
      <button id="change-value" on-click="onButtonClick">Change value</button>
    `;
  }
}

customElements.define(InjectScriptTagTemplate.is, InjectScriptTagTemplate);
