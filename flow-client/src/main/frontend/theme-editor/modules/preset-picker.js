import '@vaadin/select';
import '@vaadin/list-box';
import './preset-item.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { LumoEditor } from '../lumo-editor.js';

class PresetPicker extends PolymerElement {
  static get template() {
    return html`
      <style>
        :host {
          display: block;
        }

        vaadin-select {
          width: 100%;
        }
      </style>
      <vaadin-select id="picker" label="[[label]]" theme="dev-tools-theme-editor" placeholder="Custom">
        <slot></slot>
      </vaadin-select>
    `;
  }

  static get is() {
    return 'preset-picker';
  }

  static get properties() {
    return {
      value: {
        type: String,
        value: ''
      },
      label: String,
      name: String
    };
  }

  ready() {
    super.ready();

    document.addEventListener(LumoEditor.PROPERTY_CHANGED, (e) => {
      if (e.composedPath()[0] == this) return;

      // Wait until the property change has been handled by LumoEditor
      requestAnimationFrame(() => {
        this._updateSelectedPreset(e.detail.paletteMode || 'light');
      });
    });

    this.$.picker.addEventListener('value-changed', (e) => {
      if (!this.__preventApply) {
        this._applyPreset(this.$.picker.value);
      }
    });
  }

  _applyPreset(preset) {
    var item = this._getItemByValue(preset);

    if (!item) {
      console.warn('No preset found with value', preset, 'in', this);
      return;
    }

    this.dispatchEvent(
      new CustomEvent(LumoEditor.PROPERTY_CHANGED, {
        detail: {
          properties: item.properties
        },
        bubbles: true,
        composed: true
      })
    );
  }

  _getItemByValue(value) {
    if (this.$.picker._items) {
      return this.$.picker._items.find((item) => {
        if (item.value == value) {
          return item;
        }
      });
    }
    return null;
  }

  _updateSelectedPreset(paletteMode) {
    const paletteEditor = this.getRootNode().host;
    const lumoEditor = paletteEditor.getRootNode().host;
    var theme = lumoEditor.properties;
    var matchesPreset;

    this.$.picker._items.forEach((preset) => {
      if (matchesPreset) return;

      for (var prop in preset.properties) {
        var presetValue = preset.properties[prop];

        if (!presetValue) {
          matchesPreset = theme['global'][prop] == presetValue && theme[paletteMode][prop] == presetValue;
        } else {
          matchesPreset = theme['global'][prop] == presetValue || theme[paletteMode][prop] == presetValue;
        }

        if (!matchesPreset) {
          break;
        }
      }

      if (matchesPreset) {
        this.__preventApply = true;
        this.$.picker.value = preset.value;
        delete this.__preventApply;
        return;
      }
    });

    if (!matchesPreset) {
      this.__preventApply = true;
      this.$.picker.value = '-';
      delete this.__preventApply;
    }
  }
}

customElements.define(PresetPicker.is, PresetPicker);
