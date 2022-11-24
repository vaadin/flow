import { EditorModule } from './editor-module.js';
import './preset-picker.js';
import '../presets/default-palette.js';
import '../presets/orange-palette.js';
import '../presets/brown-and-gold-palette.js';
import '../presets/brown-and-gold-dark-palette.js';
import '../presets/purple-palette.js';
import '../presets/purple-dark-palette.js';
import '../property-editors/color-editor.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { colorToRgba } from './colors';

import { LumoEditor } from '../lumo-editor.js';
class Palette extends EditorModule {
  static get template() {
    return html`
      <style include="shared-editor-module-styles">
        :host {
          display: block;
          font-size: var(--lumo-font-size-m);
        }

        .mode-label {
          font-size: var(--lumo-font-size-s);
          font-weight: 500;
          color: var(--lumo-secondary-text-color);
        }

        .mode {
          display: flex;
          flex-wrap: wrap;
          margin: 0.5em 0;
          border-radius: 4px;
          -webkit-user-select: none;
          -moz-user-select: none;
          -ms-user-select: none;
          user-select: none;
        }

        .mode > div {
          flex: 1;
          position: relative;
        }

        .mode label {
          display: flex;
          align-items: center;
          font-weight: 500;
          font-size: 14px;
          line-height: 28px;
          background-color: var(--lumo-contrast-40pct);
          color: var(--dev-tools-background-color-active);
          border-radius: var(--lumo-border-radius-m) 0 0 var(--lumo-border-radius-m);
          padding: 0 1em;
        }

        .mode input:focus-visible + label {
          outline: 2px solid var(--lumo-primary-color);
        }

        .mode input:checked + label {
          background-color: var(--lumo-contrast-90pct);
          color: var(--dev-tools-background-color-active);
        }

        .mode button {
          float: right;
          font: inherit;
          font-size: 12px;
          background: transparent;
          border: 0;
          -webkit-appearance: none;
          appearance: none;
          padding: 0 0.7em;
          margin: 2px;
          height: 24px;
          margin-top: -26px;
          border-radius: var(--lumo-border-radius-m);
          cursor: pointer;
          color: var(--lumo-header-text-color);
          background-color: var(--lumo-base-color);
        }

        .mode button:hover {
          opacity: 0.8;
        }

        .mode button:disabled {
          color: var(--dev-tools-background-color-active);
          background-color: transparent;
          pointer-events: none;
        }

        .mode input:not(:checked) ~ button {
          visibility: hidden !important;
        }

        .mode input {
          position: absolute;
          opacity: 0;
          pointer-events: none;
        }

        .mode :last-child label {
          border-radius: 0 var(--lumo-border-radius-m) var(--lumo-border-radius-m) 0;
        }

        details div {
          display: flex;
          flex-wrap: wrap;
          justify-content: space-around;
        }

        details div color-editor {
          min-width: 20%;
          flex: 1;
          margin: 0 0 0.5em;
        }
      </style>

      <div class="mode-label">Mode</div>
      <main class="mode">
        <div>
          <input type="radio" name="mode" id="light" checked="" />
          <label for="light">Light</label>
          <button id="default-light"></button>
        </div>

        <div>
          <input type="radio" name="mode" id="dark" />
          <label for="dark">Dark</label>
          <button id="default-dark"></button>
        </div>
      </main>

      <preset-picker label="Preset">
        <template>
          <vaadin-list-box>
            <default-palette value="">Default</default-palette>
            <orange-palette value="orange">Orange</orange-palette>
            <brown-and-gold-palette value="brown" light>Brown &amp; Gold</brown-and-gold-palette>
            <brown-and-gold-dark-palette value="brown-dark" dark>Brown &amp; Gold, Dark</brown-and-gold-dark-palette>
            <purple-palette value="purple" light>Purple</purple-palette>
            <purple-dark-palette value="purple-dark" dark>Purple Dark</purple-dark-palette>
            <preset-item value="-" disabled style="height: 48px;">Custom</preset-item>
          </vaadin-list-box>
          <style>
            preset-item[disabled] {
              display: none;
            }
          </style>
        </template>
      </preset-picker>

      <details open="">
        <summary>Basic</summary>
        <div>
          <color-editor name="--lumo-base-color">Base</color-editor>
          <color-editor name="--lumo-shade">Shade</color-editor>
          <color-editor name="--lumo-tint">Tint</color-editor>
        </div>
      </details>

      <details open="">
        <summary>Primary</summary>
        <div>
          <color-editor name="--lumo-primary-color">Main</color-editor>
          <color-editor name="--lumo-primary-contrast-color">Contrast</color-editor>
          <color-editor name="--lumo-primary-text-color">Text</color-editor>
        </div>
      </details>

      <details open="">
        <summary>Error</summary>
        <div>
          <color-editor name="--lumo-error-color">Main</color-editor>
          <color-editor name="--lumo-error-contrast-color">Contrast</color-editor>
          <color-editor name="--lumo-error-text-color">Text</color-editor>
        </div>
      </details>

      <details open="">
        <summary>Success</summary>
        <div>
          <color-editor name="--lumo-success-color">Main</color-editor>
          <color-editor name="--lumo-success-contrast-color">Contrast</color-editor>
          <color-editor name="--lumo-success-text-color">Text</color-editor>
        </div>
      </details>

      <details>
        <summary>Text</summary>
        <div>
          <color-editor name="--lumo-header-text-color">Heading</color-editor>
          <color-editor name="--lumo-body-text-color">Body</color-editor>
          <color-editor name="--lumo-secondary-text-color">Secondary</color-editor>
          <color-editor name="--lumo-tertiary-text-color">Tertiary</color-editor>
          <color-editor name="--lumo-disabled-text-color">Disabled</color-editor>
        </div>
      </details>

      <details>
        <summary>Shade</summary>
        <div>
          <color-editor name="--lumo-shade-90pct">90%</color-editor>
          <color-editor name="--lumo-shade-80pct">80%</color-editor>
          <color-editor name="--lumo-shade-70pct">70%</color-editor>
          <color-editor name="--lumo-shade-60pct">60%</color-editor>
          <color-editor name="--lumo-shade-50pct">50%</color-editor>
          <color-editor name="--lumo-shade-40pct">40%</color-editor>
          <color-editor name="--lumo-shade-30pct">30%</color-editor>
          <color-editor name="--lumo-shade-20pct">20%</color-editor>
          <color-editor name="--lumo-shade-10pct">10%</color-editor>
          <color-editor name="--lumo-shade-5pct">5%</color-editor>
        </div>
      </details>

      <details>
        <summary>Tint</summary>
        <div>
          <color-editor name="--lumo-tint-90pct">90%</color-editor>
          <color-editor name="--lumo-tint-80pct">80%</color-editor>
          <color-editor name="--lumo-tint-70pct">70%</color-editor>
          <color-editor name="--lumo-tint-60pct">60%</color-editor>
          <color-editor name="--lumo-tint-50pct">50%</color-editor>
          <color-editor name="--lumo-tint-40pct">40%</color-editor>
          <color-editor name="--lumo-tint-30pct">30%</color-editor>
          <color-editor name="--lumo-tint-20pct">20%</color-editor>
          <color-editor name="--lumo-tint-10pct">10%</color-editor>
          <color-editor name="--lumo-tint-5pct">5%</color-editor>
        </div>
      </details>
    `;
  }

  static get is() {
    return 'palette-editor';
  }

  static get properties() {
    return {
      mode: {
        type: String,
        value: 'light',
        observer: '_modeChanged'
      }
    };
  }
  get lumoEditor() {
    const lumoEditor = this.getRootNode().host;
    return lumoEditor;
  }
  ready() {
    super.ready();

    // Light/dark mode selector
    const mode = this.shadowRoot.querySelector('.mode');
    mode.addEventListener('change', (e) => {
      this.mode = e.target.id;
    });

    const light = this.shadowRoot.querySelector('#default-light');
    const dark = this.shadowRoot.querySelector('#default-dark');

    light.addEventListener('click', () => this.setDefaultThemePalette('light', true));
    dark.addEventListener('click', () => this.setDefaultThemePalette('dark', true));

    const documentDark = document.documentElement.getAttribute('theme') === 'dark';
    this.setDefaultThemePalette(documentDark ? 'dark' : 'light', false);

    this.lumoEditor.addEventListener(LumoEditor.PROPERTY_CHANGED, (e) => {
      var entry = e.detail;

      if (e.composedPath().indexOf(this) > -1) {
        entry.paletteMode = this.mode;
      } else if (entry.paletteMode) {
        this.mode = entry.paletteMode;
      }

      for (var prop in entry.properties) {
        if (prop == '--lumo-tint') {
          this._processTintShade(entry, 'tint');
        } else if (prop == '--lumo-shade') {
          this._processTintShade(entry, 'shade');
        } else if (prop == '--lumo-primary-color') {
          this._processPrimaryErrorSuccessColor(entry, 'primary');
        } else if (prop == '--lumo-error-color') {
          this._processPrimaryErrorSuccessColor(entry, 'error');
        } else if (prop == '--lumo-success-color') {
          this._processPrimaryErrorSuccessColor(entry, 'success');
        }
      }
    });
  }

  setDefaultThemePalette(palette, emitEvent) {
    const documentDark = palette == 'dark';
    const light = this.shadowRoot.querySelector('#default-light');
    const dark = this.shadowRoot.querySelector('#default-dark');

    setTimeout(() => (this.mode = documentDark ? 'dark' : 'light'), 0);

    if (documentDark) {
      light.innerText = 'Set as default';
      dark.innerText = 'Default';
    } else {
      dark.innerText = 'Set as default';
      light.innerText = 'Default';
    }

    dark.toggleAttribute('disabled', documentDark);
    light.toggleAttribute('disabled', !documentDark);

    if (emitEvent) {
      this.dispatchEvent(
        new CustomEvent('default-theme-palette-updated', {
          detail: { palette },
          bubbles: true,
          composed: true
        })
      );
    }
  }
  _modeChanged(newVal, oldVal) {
    if (oldVal == undefined) return;

    this.shadowRoot.querySelector('#' + newVal).checked = true;
    document.documentElement.setAttribute('theme', this.mode);

    // TODO this feels like a hack

    Array.from(this.shadowRoot.querySelectorAll('color-editor')).forEach((editor) => {
      editor.mode = this.mode;
    });

    this.shadowRoot.querySelector('preset-picker')._updateSelectedPreset(this.mode);
  }

  _processPrimaryErrorSuccessColor(entry, which) {
    var prop = `--lumo-${which}-color`;
    var val = entry.properties[prop];
    var dependentEntry = {
      properties: {},
      paletteMode: entry.paletteMode,
      history: true
    };

    var dependentPropText = `--lumo-${which}-text-color`;
    var dependentProp50 = `--lumo-${which}-color-50pct`;
    var dependentProp10 = `--lumo-${which}-color-10pct`;

    if (val) {
      entry.properties[dependentPropText] = dependentEntry.properties[dependentPropText] = colorToRgba(val, 1);
      entry.properties[dependentProp50] = dependentEntry.properties[dependentProp50] = colorToRgba(val, 0.5);
      entry.properties[dependentProp10] = dependentEntry.properties[dependentProp10] = colorToRgba(val, 0.1);
    } else {
      entry.properties[dependentPropText] = dependentEntry.properties[dependentPropText] = undefined;
      entry.properties[dependentProp50] = dependentEntry.properties[dependentProp50] = undefined;
      entry.properties[dependentProp10] = dependentEntry.properties[dependentProp10] = undefined;
    }

    this._notifyPropertyChange(dependentEntry);
  }

  _processTintShade(entry, which) {
    var prop = `--lumo-${which}`;
    var val = entry.properties[prop];
    var dependentEntry = {
      properties: {},
      paletteMode: entry.paletteMode,
      history: true
    };

    for (var i = 5; i < 100; i += 10) {
      var dependentProp = `--lumo-${which}-${i}pct`;
      if (val) {
        // TODO needs a better formula (than just a linear progression of opacity)
        entry.properties[dependentProp] = dependentEntry.properties[dependentProp] = colorToRgba(val, i / 100);
      } else {
        entry.properties[dependentProp] = dependentEntry.properties[dependentProp] = undefined;
      }
      if (i == 5) i = 0;
    }

    this._notifyPropertyChange(dependentEntry);
  }
}

customElements.define(Palette.is, Palette);
