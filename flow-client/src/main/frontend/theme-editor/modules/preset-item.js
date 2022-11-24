import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { ItemMixin } from '@vaadin/item/vaadin-item-mixin.js';
import { PolymerElement } from '@polymer/polymer/polymer-element.js';

const $_documentContainer = document.createElement('template');

$_documentContainer.innerHTML = `<dom-module id="preset-item">
      <template>
        <style include="lumo-item">
          :host {
            outline: none;
            border-radius: var(--lumo-border-radius);
            padding: 0.25em 1em;
          }
        </style>
      </template>
    </dom-module>`;

document.body.appendChild($_documentContainer.content);

export class PresetItem extends ItemMixin(PolymerElement) {
  static get template() {
    return html`
      <style include="preset-item"></style>
      <slot></slot>
    `;
  }

  static get is() {
    return 'preset-item';
  }

  static get properties() {
    return {
      properties: {
        type: Object,
        observer: '_propsChanged'
      },
      preview: Boolean
    };
  }

  _propsChanged() {
    if (this.preview) {
      for (var prop in this.properties) {
        if (this.properties[prop]) {
          this.style.setProperty(prop, this.properties[prop]);
        }
      }
    }
  }
}

customElements.define(PresetItem.is, PresetItem);
