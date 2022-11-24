import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { LumoEditor } from '../lumo-editor.js';
const $_documentContainer = document.createElement('template');

$_documentContainer.innerHTML = `<dom-module id="property-editor-shared-styles">
  <template>
    <style>
      .clear-button {
        width: 20px;
        height: 20px;
        margin-left: 4px;
        padding: 0;
        border: 0;
        background-color: transparent;
        color: var(--lumo-tertiary-text-color);
        outline: none;
        border-radius: var(--lumo-border-radius);
        flex: none;
      }

      .clear-button:focus,
      .clear-button:hover {
        color: var(--lumo-body-text-color);
      }

      .clear-button:active {
        background-color: var(--lumo-contrast-10pct);
      }

      .clear-button iron-icon {
        width: 20px;
        height: 20px;
        transform: scaleX(-1);
      }

      :host(.undefined) .clear-button {
        display: none;
      }

      input.editor {
        background-color: var(--lumo-contrast-10pct);
        font: inherit;
        font-weight: 500;
        color: inherit;
        padding: 0 0.5em;
        height: var(--lumo-size-s);
        border: 0;
        border-radius: var(--lumo-border-radius);
        outline: none;
        flex: 1;
        width: 100%;
        box-sizing: border-box;
      }

      input.editor:hover:not(:focus) {
        background-color: var(--lumo-contrast-20pct);
      }

      input.editor:focus {
        box-shadow: 0 0 0 2px var(--lumo-primary-color-50pct);
      }
    </style>
  </template>
</dom-module>`;

document.head.appendChild($_documentContainer.content);
export class PropertyEditor extends PolymerElement {
  static get template() {
    return html`
      <style include="property-editor-shared-styles">
        :host {
          display: inline-flex;
          align-items: center;
          position: relative;
        }

        .clear-button {
          position: absolute;
          right: calc(var(--lumo-size-s) / 2 - 10px);
        }

        input.editor {
          padding-right: calc(20px + 0.5em);
        }
      </style>
      <input
        type="text"
        value="{{value::change}}"
        placeholder="[[_valueOrDefault(value,forceValueUpdate)]]"
        class="editor"
      />
      <button on-click="_clear" title="Reset to default" class="clear-button">
        <iron-icon icon="lumo:reload"></iron-icon>
      </button>
    `;
  }

  static get is() {
    return 'property-editor';
  }

  static get properties() {
    return {
      name: String,

      value: {
        type: String,
        observer: '_valueChanged'
      },

      forceValueUpdate: {
        type: Number,
        value: 0
      }
    };
  }

  ready() {
    super.ready();
    this._updateUndefinedClass();
    this.value = '';

    document.addEventListener(LumoEditor.PROPERTY_CHANGED, (e) => {
      if (e.composedPath()[0] == this) return;

      // If the property change event contains a new value for this editor, update the value
      for (var prop in e.detail.properties) {
        if (prop == this.name) {
          this._propertyChangeUpdate(e.detail);
          break;
        }
      }
    });
  }

  _clear(e) {
    e.stopPropagation();
    this.value = undefined;
  }

  _computedValue() {
    if (!this.lumoEditor.previewDocument) {
      return '';
    }
    return getComputedStyle(this.lumoEditor.previewDocument.documentElement).getPropertyValue(this.name).trim();
  }
  get lumoEditor() {
    const lumoEditor = this.getRootNode().host.getRootNode().host;
    return lumoEditor;
  }

  _valueChanged() {
    this._updateUndefinedClass();

    if (!this.__preventNotify) {
      this._notifyPropertyChange();
    }
  }

  _updateUndefinedClass() {
    if (this.value) {
      this.classList.remove('undefined');
    } else {
      this.classList.add('undefined');
    }
  }

  _forceValueUpdate() {
    this.forceValueUpdate++;
  }

  _notifyPropertyChange() {
    var props = {};
    props[this.name] = this.value;

    this.dispatchEvent(
      new CustomEvent(LumoEditor.PROPERTY_CHANGED, {
        detail: {
          properties: props
        },
        bubbles: true,
        composed: true
      })
    );
  }

  _valueOrDefault(value) {
    if (value) return value;
    else return this._defaultValue();
  }

  _defaultValue() {
    return this.lumoEditor.getDefault(this.name, this.mode);
  }

  _propertyChangeUpdate(entry) {
    this.__preventNotify = true;
    this.value = entry.properties[this.name];
    delete this.__preventNotify;
  }
}

customElements.define(PropertyEditor.is, PropertyEditor);
