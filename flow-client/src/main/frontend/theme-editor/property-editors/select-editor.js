import { PropertyEditor } from './property-editor.js';
import '@vaadin/select';
import '@vaadin/list-box';
import { DomModule } from '@polymer/polymer/lib/elements/dom-module.js';
const $_documentContainer = document.createElement('template');

$_documentContainer.innerHTML = `<dom-module id="select-editor-extension">
  <template>
    <style>
      :host {
        width: 100%;
      }

      vaadin-select {
        flex: auto;
        min-width: 0;
      }

      .clear-button {
        top: 1em;
      }
    </style>
    <vaadin-select value="[[_valueForSelect(value)]]" label="[[label]]" placeholder="[[placeholder]]" theme="editor">
      <slot></slot>
    </vaadin-select>
  </template>
  
</dom-module>`;

document.head.appendChild($_documentContainer.content);
let seTemplate;

class SelectEditor extends PropertyEditor {
  static get is() {
    return 'select-editor';
  }

  static get properties() {
    return {
      label: String,
      placeholder: String
    }
  }

  static get template() {
    if (!seTemplate) {
      seTemplate = super.template.cloneNode(true);
      const thisTemplate = DomModule.import(this.is + '-extension', 'template');
      const input = seTemplate.content.querySelector('.editor');
      seTemplate.content.insertBefore(thisTemplate.content.cloneNode(true), input);
      seTemplate.content.removeChild(input);
    }
    return seTemplate;
  }

  ready() {
    super.ready();
    this.shadowRoot.querySelector('vaadin-select').addEventListener('value-changed', e => {
      this.value = e.target.value;
    });
  }

  _valueForSelect(value) {
    if (value === undefined) {
      return '';
    }
    return value;
  }
}

customElements.define(SelectEditor.is, SelectEditor);
