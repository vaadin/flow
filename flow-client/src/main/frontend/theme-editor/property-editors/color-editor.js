import { PropertyEditor } from './property-editor.js';
import '@fooloomanzoo/color-picker/color-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { Debouncer } from '@polymer/polymer/lib/utils/debounce.js';
import { timeOut } from '@polymer/polymer/lib/utils/async.js';
class ColorEditor extends PropertyEditor {
  static get template() {
    return html`
      <style include="property-editor-shared-styles">
        :host {
          display: inline-flex;
          flex-direction: column;
          align-items: center;
          vertical-align: middle;
          margin: 4px;
          cursor: default;
          -webkit-user-select: none;
          -moz-user-select: none;
          -ms-user-select: none;
          user-select: none;
          font-size: 11px;
          font-weight: 500;
          color: var(--lumo-tertiary-text-color);
        }

        .preview {
          width: 32px;
          height: 32px;
          box-shadow: inset 0 0 0 2px var(--lumo-contrast-5pct), inset 0 0 0 2px var(--lumo-base-color),
            0 0 0 2px var(--lumo-contrast-30pct);
          padding: 2px;
          box-sizing: border-box;
          background-clip: content-box;
          border-radius: 8px;
          flex: none;
          overflow: hidden;
          position: relative;
        }

        :host(:not(.undefined)) .preview::after {
          content: '';
          position: absolute;
          top: -8px;
          right: -8px;
          width: 16px;
          height: 16px;
          background-color: var(--lumo-base-color);
          background-image: linear-gradient(var(--lumo-contrast-5pct), var(--lumo-contrast-5pct));
          transform: rotate(45deg);
          border: 2px solid transparent;
          border-bottom-color: var(--lumo-contrast-10pct);
          background-clip: content-box;
        }

        .text {
          margin-top: 2px;
          max-width: 100%;
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
        }
      </style>

      <div class="preview" style="background-color: [[_valueOrDefault(value,mode,forceValueUpdate)]];"></div>
      <div class="text">
        <slot></slot>
      </div>

      <vaadin-overlay id="overlay" theme="editor" on-opened-changed="_openedChanged">
        <template>
          <style>
            color-element {
              display: block;
            }

            button iron-icon {
              transform: scaleX(-1);
            }

            .footer {
              display: flex;
              align-items: center;
              margin-top: var(--lumo-space-xs);
            }
          </style>
          <color-element
            id="picker"
            hide-random-button=""
            alpha-mode="true"
            value="[[_previewValue]]"
            on-value-changed="_pickerValueChanged"
          ></color-element>
          <div class="footer">
            <input type="text" value="[[_previewValue]]" on-change="_inputValueChanged" class="editor" />
            <button on-click="_clear" class="clear-button" title="Reset to default">
              <iron-icon icon="lumo:reload"></iron-icon>
            </button>
          </div>
        </template>
      </vaadin-overlay>
    `;
  }

  static get is() {
    return 'color-editor';
  }

  static get properties() {
    return {
      _value: {
        type: Object,
        value: function () {
          return {
            light: undefined,
            dark: undefined
          };
        }
      },

      _previewValue: {
        type: String,
        nofity: true
      },

      mode: {
        type: String,
        value: 'light',
        observer: '_modeChanged'
      }
    };
  }

  _modeChanged() {
    this.__preventNotify = true;
    this.value = this._value[this.mode];
    delete this.__preventNotify;
  }
  _valueChanged() {
    this._value[this.mode || 'light'] = this.value;
    super._valueChanged();
  }

  ready() {
    super.ready();
    // TODO add keyboard support
    this.addEventListener('click', (e) => {
      this.$.overlay.opened = true;
    });
  }

  _openedChanged(e) {
    if (e.detail.value) {
      // opened
      this._updatePickerValue();
    }
  }

  _clear(e) {
    this.value = undefined;
    this._updatePickerValue();
  }

  _propertyChangeUpdate(entry) {
    var mode = entry.paletteMode || this.mode;
    this._value[mode] = entry.properties[this.name];
    if (this.mode == entry.paletteMode || this.mode) {
      this.__preventNotify = true;
      this.value = entry.properties[this.name];
      delete this.__preventNotify;
    }
  }

  _updatePickerValue() {
    this._previewValue = this._computedValue();
  }

  _inputValueChanged(e) {
    this._previewValue = e.target.value;
    this.value = this._previewValue;
  }

  _pickerValueChanged(e) {
    this._previewValue = e.detail.value;
    this._debounceNotify = Debouncer.debounce(this._debounceNotify, timeOut.after(300), () => {
      this.value = this._previewValue;
    });
  }
}

customElements.define(ColorEditor.is, ColorEditor);
