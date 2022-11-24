import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';
import '@vaadin/vaadin-lumo-styles/icons.js';
import './modules/palette-editor.js';
import './modules/typography-editor.js';
import './modules/style-editor.js';
import './modules/size-and-space-editor.js';
import { html, htmlLiteral } from '@polymer/polymer/lib/utils/html-tag.js';
import { DomModule } from '@polymer/polymer/lib/elements/dom-module.js';
import { registerStyles, css, unsafeCSS } from '@vaadin/vaadin-themable-mixin';
import './login-to-access.js';
const $_documentContainer = document.createElement('template');
$_documentContainer.innerHTML = `<dom-module id="shared-editor-module-styles">
  <template>
    <style include="lumo-typography">
      :host {
        display: block;
        font-size: var(--lumo-font-size-m);
      }

      h6 {
        margin: 10px 22px;
        color: var(--lumo-tertiary-text-color);
      }

      details {
        background-color: var(--lumo-contrast-5pct);
        padding: 1em;
        border-radius: var(--lumo-border-radius);
        overflow: auto;
        margin: 2px 0;
      }

      details summary {
        cursor: default;
        font-weight: 500;
        color: var(--lumo-secondary-text-color);
        margin: -1em;
        padding: 0 1em;
        height: var(--lumo-size-m);
        line-height: var(--lumo-size-m);
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
        user-select: none;
      }

      details summary:hover {
        background-color: var(--lumo-contrast-5pct);
      }

      details[open] summary {
        margin-bottom: 1em;
      }

      table {
        width: 100%;
      }

      th {
        padding: 0;
        font-size: var(--lumo-font-size-s);
        font-weight: 400;
        text-align: center;
        color: var(--lumo-secondary-text-color);
      }

      thead th {
        font-weight: 600;
        color: var(--lumo-body-text-color);
      }

      tbody th {
        text-align: right;
        padding-right: 1em;
      }

      th:first-child {
        width: 6em;
      }

      thead th:first-child {
        text-align: left;
      }

      td {
        text-align: center;
      }

      table property-editor {
        width: 100%;
        font-size: var(--lumo-font-size-s);
      }

      .help-text {
        font-size: var(--lumo-font-size-s);
        color: var(--lumo-tertiary-text-color);
        margin-top: 0;
        margin-bottom: 2em;
      }
    </style>
  </template>
</dom-module><dom-module id="editor" theme-for="vaadin-combo-box vaadin-select">
  <template>
    <style>
      :host([theme~="editor"]) [part="clear-button"] {
        display: none;
      }
    </style>
  </template>
</dom-module>`;

const editorLumoPropertyOverrides = htmlLiteral`
:host {
  --lumo-font-family: var(--dev-tools-font-family);
  --lumo-font-size-xs: var(--dev-tools-font-size-small);
  --lumo-font-size-s: var(--dev-tools-font-size-small);
  --lumo-font-size-m: var(--dev-tools-font-size);

  --lumo-size-xs: 20px;
  --lumo-size-s: 24px;
  --lumo-size-m: 30px;
  --lumo-size-l: 36px;
  --lumo-size-xl: 44px;
  --lumo-space-xs: 4px;
  --lumo-space-s: 8px;
  --lumo-space-m: 16px;
  --lumo-space-l: 24px;
  --lumo-space-xl: 32px;

  --lumo-header-text-color: var(--dev-tools-text-color-emphasis);
  --lumo-body-text-color: var(--dev-tools-text-color);
  --lumo-secondary-text-color: var(--dev-tools-text-color-secondary);
  --lumo-tertiary-text-color: var(--dev-tools-text-color-secondary);
  --lumo-disabled-text-color: rgba(255, 255, 255, 0.4);
  --lumo-primary-color: var(--dev-tools-blue-color);
  --lumo-primary-color-50pct: var(--dev-tools-blue-color);
  --lumo-primary-text-color: var(--dev-tools-blue-color);
  --lumo-error-color: var(--dev-tools-red-color);
  --lumo-error-color-50pct: var(--dev-tools-red-color);
  --lumo-error-text-color: var(--dev-tools-red-color);
  --lumo-success-color: var(--dev-tools-green-color);
  --lumo-success-color-50pct: var(--dev-tools-green-color);
  --lumo-success-text-color: var(--dev-tools-green-color);

  --lumo-base-color: var(--dev-tools-background-color-active);

  --lumo-tint-5pct: rgba(255, 255, 255, 0.04);
  --lumo-tint-10pct: rgba(255, 255, 255, 0.08);
  --lumo-tint-20pct: rgba(255, 255, 255, 0.14);
  --lumo-tint-30pct: rgba(255, 255, 255, 0.25);
  --lumo-tint-40pct: rgba(255, 255, 255, 0.36);
  --lumo-tint-50pct: rgba(255, 255, 255, 0.47);
  --lumo-tint-60pct: rgba(255, 255, 255, 0.58);
  --lumo-tint-70pct: rgba(255, 255, 255, 0.69);
  --lumo-tint-80pct: rgba(255, 255, 255, 0.80);
  --lumo-tint-90pct: rgba(255, 255, 255, 0.9);
  --lumo-tint: rgba(255, 255, 255, 1);

  --lumo-contrast-5pct: var(--lumo-tint-5pct);
  --lumo-contrast-10pct: var(--lumo-tint-10pct);
  --lumo-contrast-20pct: var(--lumo-tint-20pct);
  --lumo-contrast-30pct: var(--lumo-tint-30pct);
  --lumo-contrast-40pct: var(--lumo-tint-40pct);
  --lumo-contrast-50pct: var(--lumo-tint-50pct);
  --lumo-contrast-60pct: var(--lumo-tint-60pct);
  --lumo-contrast-70pct: var(--lumo-tint-70pct);
  --lumo-contrast-80pct: var(--lumo-tint-80pct);
  --lumo-contrast-90pct: var(--lumo-tint-90pct);
  --lumo-contrast: var(--lumo-tint);

  --lumo-border-radius-s: 3px;
  --lumo-border-radius-m: 6px;
  --lumo-border-radius-l: 9px;
}
`;

document.head.appendChild($_documentContainer.content);
export class LumoEditor extends PolymerElement {
  static get template() {
    return html`
      <style include="lumo-color lumo-typography">
        :host {
          display: block;
          overflow: auto;
          height: 80vh;
          font-family: var(--dev-tools-font-family);
          font-size: var(--dev-tools-font-size);
          position: relative;
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
        }

        ${editorLumoPropertyOverrides} .tools {
          display: flex;
          align-items: center;
          background: var(--lumo-contrast-5pct);
          height: 2em;
          padding: 0.5em;
        }
        .tools[hidden] {
          display: none;
        }

        .tools button {
          border: 0;
          background: transparent;
          font: inherit;
          color: var(--lumo-tertiary-text-color);
          min-width: 2em;
          height: 2em;
          padding: 0;
          margin: 0 0.5em 0 0;
          outline: none;
          border-radius: var(--lumo-border-radius);
        }

        .tools button:disabled {
          pointer-events: none;
          color: var(--lumo-disabled-text-color);
        }

        .tools button:focus,
        .tools button:hover {
          color: var(--lumo-body-text-color);
          background-color: var(--lumo-contrast-5pct);
        }

        .tools button:active {
          background-color: var(--lumo-contrast-10pct);
        }

        .tools button iron-icon {
          vertical-align: -0.5em;
        }

        .tools .reset iron-icon {
          transform: scaleX(-1);
        }

        .tools .primary:not(:disabled) {
          background-color: var(--lumo-primary-color);
          color: var(--lumo-primary-contrast-color);
        }

        .tools .divider {
          width: 1px;
          background-color: var(--lumo-contrast-10pct);
          height: 20px;
          margin: 0 10px;
        }

        .tabs {
          display: flex;
          flex-wrap: wrap;
          height: calc(100% - 3em);
        }

        .tab {
          flex: auto;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-weight: 500;
          font-size: 14px;
          color: var(--lumo-tertiary-text-color);
          white-space: nowrap;
          height: 3em;
          -webkit-user-select: none;
          -moz-user-select: none;
          -ms-user-select: none;
          user-select: none;
        }

        .tabs > input:focus-visible + .tab {
          outline: 2px solid var(--lumo-primary-color);
        }

        .tabs > input:checked + .tab {
          color: var(--lumo-header-text-color);
        }

        .tabs > input {
          position: absolute;
          opacity: 0;
          pointer-events: none;
        }

        .tab-content {
          order: 1;
          width: 100%;
          padding: 0 16px 32px;
          box-sizing: border-box;
          height: calc(100% - 3em);
          overflow: auto;
          -webkit-overflow-scrolling: touch;
        }

        .tabs > input:not(:checked) + .tab + .tab-content {
          display: none;
        }

        #output {
          display: flex;
          flex: auto;
          width: 100%;
          max-height: 100%;
          font-family: monospace;
          white-space: pre;
        }
      </style>

      <div class="tools" hidden="[[hideTools]]">
        <button on-click="undo" class="undo" title="Undo (Ctrl + Z / ⌘Z)" disabled="">
          <iron-icon icon="lumo:undo"></iron-icon>
        </button>
        <button on-click="redo" class="redo" title="Redo (Ctrl + Y / ⇧⌘Z)" disabled="">
          <iron-icon icon="lumo:redo"></iron-icon>
        </button>
        <div class="divider"></div>
        <button on-click="_confirmReset" class="reset" title="Reset all" disabled="">
          <iron-icon icon="lumo:reload"></iron-icon>
        </button>
      </div>

      <main class="tabs">
        <!-- Color -->
        <input type="radio" name="main-tabs" id="color" checked="" />
        <label for="color" class="tab">Color</label>
        <section class="tab-content">
          <palette-editor></palette-editor>
        </section>

        <!-- Typography -->
        <input type="radio" name="main-tabs" id="typography" />
        <label for="typography" class="tab">Typography</label>
        <section class="tab-content">
          <typography-editor></typography-editor>
        </section>

        <!-- Style -->
        <input type="radio" name="main-tabs" id="style" />
        <label for="style" class="tab">Style</label>
        <section class="tab-content">
          <style-editor></style-editor>
        </section>

        <!-- Sixing & Spacing -->
        <input type="radio" name="main-tabs" id="ss" />
        <label for="ss" class="tab">Size &amp; Space</label>
        <section class="tab-content">
          <size-and-space-editor></size-and-space-editor>
        </section>

        <slot></slot>
      </main>
    `;
  }

  static get PROPERTY_CHANGED() {
    return 'propertychanged';
  }

  static get is() {
    return 'lumo-editor';
  }

  static get properties() {
    return {
      historyEntries: {
        type: Array,
        value: []
      },

      historyIndex: {
        type: Number,
        value: -1
      },

      properties: {
        type: Object,
        value: {
          global: {},
          light: {},
          dark: {}
        }
      },

      hideTools: {
        type: Boolean
      }
    };
  }

  getDefault(name, mode) {
    if (!mode) {
      mode = 'light';
    }
    if (!this.defaults[mode]) {
      console.error("Missing defaults for mode '" + mode + "'");
      return '';
    } else if (!this.defaults[mode][name]) {
      console.error("Missing defaults for '" + name + "' in mode " + mode);
      return '';
    }

    // return this.defaults[mode][name];
    return window.getComputedStyle(document.documentElement).getPropertyValue(name);
  }
  constructor() {
    super();
    this.defaults = {};
    this.defaults['dark'] = {
      '--lumo-base-color': 'hsl(214, 35%, 21%)',
      '--lumo-body-text-color': 'hsla(214, 96%, 96%, 0.9)',
      '--lumo-border-radius': '0.25em',
      '--lumo-contrast': 'hsl(214, 100%, 98%)',
      '--lumo-contrast-10pct': 'hsla(214, 60%, 80%, 0.14)',
      '--lumo-contrast-20pct': 'hsla(214, 64%, 82%, 0.23)',
      '--lumo-contrast-30pct': 'hsla(214, 69%, 84%, 0.32)',
      '--lumo-contrast-40pct': 'hsla(214, 73%, 86%, 0.41)',
      '--lumo-contrast-50pct': 'hsla(214, 78%, 88%, 0.5)',
      '--lumo-contrast-5pct': 'hsla(214, 65%, 85%, 0.06)',
      '--lumo-contrast-60pct': 'hsla(214, 82%, 90%, 0.6)',
      '--lumo-contrast-70pct': 'hsla(214, 87%, 92%, 0.7)',
      '--lumo-contrast-80pct': 'hsla(214, 91%, 94%, 0.8)',
      '--lumo-contrast-90pct': 'hsla(214, 96%, 96%, 0.9)',
      '--lumo-disabled-text-color': 'hsla(214, 69%, 84%, 0.32)',
      '--lumo-error-color': 'hsl(3, 90%, 63%)',
      '--lumo-error-color-10pct': 'hsla(3, 90%, 63%, 0.1)',
      '--lumo-error-color-50pct': 'hsla(3, 90%, 63%, 0.5)',
      '--lumo-error-text-color': 'hsl(3, 100%, 67%)',
      '--lumo-header-text-color': 'hsl(214, 100%, 98%)',
      '--lumo-primary-color': 'hsl(214, 86%, 55%)',
      '--lumo-primary-color-10pct': 'hsla(214, 90%, 63%, 0.1)',
      '--lumo-primary-color-50pct': 'hsla(214, 86%, 55%, 0.5)',
      '--lumo-primary-contrast-color': '#FFF',
      '--lumo-primary-text-color': 'hsl(214, 100%, 70%)',
      '--lumo-secondary-text-color': 'hsla(214, 87%, 92%, 0.7)',
      '--lumo-shade': 'hsl(214, 33%, 13%)',
      '--lumo-shade-10pct': 'hsla(214, 4%, 2%, 0.15)',
      '--lumo-shade-20pct': 'hsla(214, 8%, 4%, 0.23)',
      '--lumo-shade-30pct': 'hsla(214, 12%, 6%, 0.32)',
      '--lumo-shade-40pct': 'hsla(214, 16%, 8%, 0.41)',
      '--lumo-shade-50pct': 'hsla(214, 20%, 10%, 0.5)',
      '--lumo-shade-5pct': 'hsla(214, 0%, 0%, 0.07)',
      '--lumo-shade-60pct': 'hsla(214, 24%, 12%, 0.6)',
      '--lumo-shade-70pct': 'hsla(214, 28%, 13%, 0.7)',
      '--lumo-shade-80pct': 'hsla(214, 32%, 13%, 0.8)',
      '--lumo-shade-90pct': 'hsla(214, 33%, 13%, 0.9)',
      '--lumo-success-color': 'hsl(145, 65%, 42%)',
      '--lumo-success-color-10pct': 'hsla(145, 65%, 42%, 0.1)',
      '--lumo-success-color-50pct': 'hsla(145, 65%, 42%, 0.5)',
      '--lumo-success-text-color': 'hsl(145, 85%, 47%)',
      '--lumo-tertiary-text-color': 'hsla(214, 78%, 88%, 0.5)',
      '--lumo-tint': 'hsl(214, 100%, 98%)',
      '--lumo-tint-10pct': 'hsla(214, 60%, 80%, 0.14)',
      '--lumo-tint-20pct': 'hsla(214, 64%, 82%, 0.23)',
      '--lumo-tint-30pct': 'hsla(214, 69%, 84%, 0.32)',
      '--lumo-tint-40pct': 'hsla(214, 73%, 86%, 0.41)',
      '--lumo-tint-50pct': 'hsla(214, 78%, 88%, 0.5)',
      '--lumo-tint-5pct': 'hsla(214, 65%, 85%, 0.06)',
      '--lumo-tint-60pct': 'hsla(214, 82%, 90%, 0.6)',
      '--lumo-tint-70pct': 'hsla(214, 87%, 92%, 0.7)',
      '--lumo-tint-80pct': 'hsla(214, 91%, 94%, 0.8)',
      '--lumo-tint-90pct': 'hsla(214, 96%, 96%, 0.9)',
      '--lumo-base-color': 'hsl(214, 35%, 21%)',
      '--lumo-body-text-color': 'hsla(214, 96%, 96%, 0.9)',
      '--lumo-border-radius': '0.25em',
      '--lumo-box-shadow-l': '0 3px 18px -2px  hsla(214, 8%, 4%, 0.23), 0 12px 48px -6px  hsla(214, 16%, 8%, 0.41)',
      '--lumo-box-shadow-m': '0 2px 6px -1px  hsla(214, 8%, 4%, 0.23), 0 8px 24px -4px  hsla(214, 16%, 8%, 0.41)',
      '--lumo-box-shadow-s': '0 1px 2px 0  hsla(214, 8%, 4%, 0.23), 0 2px 8px -2px  hsla(214, 16%, 8%, 0.41)',
      '--lumo-box-shadow-xl': '0 4px 24px -3px  hsla(214, 8%, 4%, 0.23), 0 18px 64px -8px  hsla(214, 16%, 8%, 0.41)',
      '--lumo-contrast': 'hsl(214, 100%, 98%)',
      '--lumo-contrast-10pct': 'hsla(214, 60%, 80%, 0.14)',
      '--lumo-contrast-20pct': 'hsla(214, 64%, 82%, 0.23)',
      '--lumo-contrast-30pct': 'hsla(214, 69%, 84%, 0.32)',
      '--lumo-contrast-40pct': 'hsla(214, 73%, 86%, 0.41)',
      '--lumo-contrast-50pct': 'hsla(214, 78%, 88%, 0.5)',
      '--lumo-contrast-5pct': 'hsla(214, 65%, 85%, 0.06)',
      '--lumo-contrast-60pct': 'hsla(214, 82%, 90%, 0.6)',
      '--lumo-contrast-70pct': 'hsla(214, 87%, 92%, 0.7)',
      '--lumo-contrast-80pct': 'hsla(214, 91%, 94%, 0.8)',
      '--lumo-contrast-90pct': 'hsla(214, 96%, 96%, 0.9)',
      '--lumo-disabled-text-color': 'hsla(214, 69%, 84%, 0.32)',
      '--lumo-error-color': 'hsl(3, 90%, 63%)',
      '--lumo-error-color-10pct': 'hsla(3, 90%, 63%, 0.1)',
      '--lumo-error-color-50pct': 'hsla(3, 90%, 63%, 0.5)',
      '--lumo-error-contrast-color': '#FFF',
      '--lumo-error-text-color': 'hsl(3, 100%, 67%)',
      '--lumo-font-family':
        '-apple-system, BlinkMacSystemFont, "Roboto", "Segoe UI", Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol"',
      '--lumo-font-size-l': '1.125rem',
      '--lumo-font-size-m': '1rem',
      '--lumo-font-size-s': '0.875rem',
      '--lumo-font-size-xl': '1.375rem',
      '--lumo-font-size-xs': '0.8125rem',
      '--lumo-font-size-xxl': '1.75rem',
      '--lumo-font-size-xxs': '0.75rem',
      '--lumo-font-size-xxxl': '2.5rem',
      '--lumo-header-text-color': 'hsl(214, 100%, 98%)',
      '--lumo-icon-size': '1.5em',
      '--lumo-icon-size-l': '2.25em',
      '--lumo-icon-size-m': '1.5em',
      '--lumo-icon-size-s': '1.25em',
      '--lumo-line-height-m': '1.625',
      '--lumo-line-height-s': '1.375',
      '--lumo-line-height-xs': '1.25',
      '--lumo-primary-color': 'hsl(214, 86%, 55%)',
      '--lumo-primary-color-10pct': 'hsla(214, 90%, 63%, 0.1)',
      '--lumo-primary-color-50pct': 'hsla(214, 86%, 55%, 0.5)',
      '--lumo-primary-contrast-color': '#FFF',
      '--lumo-primary-text-color': 'hsl(214, 100%, 70%)',
      '--lumo-secondary-text-color': 'hsla(214, 87%, 92%, 0.7)',
      '--lumo-shade': 'hsl(214, 33%, 13%)',
      '--lumo-shade-10pct': 'hsla(214, 4%, 2%, 0.15)',
      '--lumo-shade-20pct': 'hsla(214, 8%, 4%, 0.23)',
      '--lumo-shade-30pct': 'hsla(214, 12%, 6%, 0.32)',
      '--lumo-shade-40pct': 'hsla(214, 16%, 8%, 0.41)',
      '--lumo-shade-50pct': 'hsla(214, 20%, 10%, 0.5)',
      '--lumo-shade-5pct': 'hsla(214, 0%, 0%, 0.07)',
      '--lumo-shade-60pct': 'hsla(214, 24%, 12%, 0.6)',
      '--lumo-shade-70pct': 'hsla(214, 28%, 13%, 0.7)',
      '--lumo-shade-80pct': 'hsla(214, 32%, 13%, 0.8)',
      '--lumo-shade-90pct': 'hsla(214, 33%, 13%, 0.9)',
      '--lumo-size-l': '2.75rem',
      '--lumo-size-m': '2.25rem',
      '--lumo-size-s': '1.875rem',
      '--lumo-size-xl': '3.5rem',
      '--lumo-size-xs': '1.625rem',
      '--lumo-space-l': '1.5rem',
      '--lumo-space-m': '1rem',
      '--lumo-space-s': '0.5rem',
      '--lumo-space-tall-l': '1.5rem calc( 1.5rem / 2)',
      '--lumo-space-tall-m': '1rem calc( 1rem / 2)',
      '--lumo-space-tall-s': '0.5rem calc( 0.5rem / 2)',
      '--lumo-space-tall-xl': '2.5rem calc( 2.5rem / 2)',
      '--lumo-space-tall-xs': '0.25rem calc( 0.25rem / 2)',
      '--lumo-space-wide-l': 'calc( 1.5rem / 2)  1.5rem',
      '--lumo-space-wide-m': 'calc( 1rem / 2)  1rem',
      '--lumo-space-wide-s': 'calc( 0.5rem / 2)  0.5rem',
      '--lumo-space-wide-xl': 'calc( 2.5rem / 2)  2.5rem',
      '--lumo-space-wide-xs': 'calc( 0.25rem / 2)  0.25rem',
      '--lumo-space-xl': '2.5rem',
      '--lumo-space-xs': '0.25rem',
      '--lumo-success-color': 'hsl(145, 65%, 42%)',
      '--lumo-success-color-10pct': 'hsla(145, 65%, 42%, 0.1)',
      '--lumo-success-color-50pct': 'hsla(145, 65%, 42%, 0.5)',
      '--lumo-success-contrast-color': '#FFF',
      '--lumo-success-text-color': 'hsl(145, 85%, 47%)',
      '--lumo-tertiary-text-color': 'hsla(214, 78%, 88%, 0.5)',
      '--lumo-tint': 'hsl(214, 100%, 98%)',
      '--lumo-tint-10pct': 'hsla(214, 60%, 80%, 0.14)',
      '--lumo-tint-20pct': 'hsla(214, 64%, 82%, 0.23)',
      '--lumo-tint-30pct': 'hsla(214, 69%, 84%, 0.32)',
      '--lumo-tint-40pct': 'hsla(214, 73%, 86%, 0.41)',
      '--lumo-tint-50pct': 'hsla(214, 78%, 88%, 0.5)',
      '--lumo-tint-5pct': 'hsla(214, 65%, 85%, 0.06)',
      '--lumo-tint-60pct': 'hsla(214, 82%, 90%, 0.6)',
      '--lumo-tint-70pct': 'hsla(214, 87%, 92%, 0.7)',
      '--lumo-tint-80pct': 'hsla(214, 91%, 94%, 0.8)',
      '--lumo-tint-90pct': 'hsla(214, 96%, 96%, 0.9)',
      '--lumo-border-radius-s': 'calc(var(--lumo-border-radius-m) / 2)',
      '--lumo-border-radius-m': 'var(--lumo-border-radius)',
      '--lumo-border-radius-l': 'calc(var(--lumo-border-radius-m) * 2)'
    };

    this.defaults['light'] = {
      '--lumo-base-color': '#FFF',
      '--lumo-body-text-color': 'hsla(214, 40%, 16%, 0.94)',
      '--lumo-border-radius': '0.25em',
      '--lumo-contrast': 'hsl(214, 35%, 15%)',
      '--lumo-contrast-10pct': 'hsla(214, 57%, 24%, 0.1)',
      '--lumo-contrast-20pct': 'hsla(214, 53%, 23%, 0.16)',
      '--lumo-contrast-30pct': 'hsla(214, 50%, 22%, 0.26)',
      '--lumo-contrast-40pct': 'hsla(214, 47%, 21%, 0.38)',
      '--lumo-contrast-50pct': 'hsla(214, 45%, 20%, 0.5)',
      '--lumo-contrast-5pct': 'hsla(214, 61%, 25%, 0.05)',
      '--lumo-contrast-60pct': 'hsla(214, 43%, 19%, 0.61)',
      '--lumo-contrast-70pct': 'hsla(214, 42%, 18%, 0.72)',
      '--lumo-contrast-80pct': 'hsla(214, 41%, 17%, 0.83)',
      '--lumo-contrast-90pct': 'hsla(214, 40%, 16%, 0.94)',
      '--lumo-disabled-text-color': 'hsla(214, 50%, 22%, 0.26)',
      '--lumo-error-color': 'hsl(3, 100%, 61%)',
      '--lumo-error-color-10pct': 'hsla(3, 100%, 60%, 0.1)',
      '--lumo-error-color-50pct': 'hsla(3, 100%, 60%, 0.5)',
      '--lumo-error-text-color': 'hsl(3, 92%, 53%)',
      '--lumo-header-text-color': 'hsl(214, 35%, 15%)',
      '--lumo-primary-color': 'hsl(214, 90%, 52%)',
      '--lumo-primary-color-10pct': 'hsla(214, 90%, 52%, 0.1)',
      '--lumo-primary-color-50pct': 'hsla(214, 90%, 52%, 0.5)',
      '--lumo-primary-contrast-color': '#FFF',
      '--lumo-primary-text-color': 'hsl(214, 90%, 52%)',
      '--lumo-secondary-text-color': 'hsla(214, 42%, 18%, 0.72)',
      '--lumo-shade': 'hsl(214, 35%, 15%)',
      '--lumo-shade-10pct': 'hsla(214, 57%, 24%, 0.1)',
      '--lumo-shade-20pct': 'hsla(214, 53%, 23%, 0.16)',
      '--lumo-shade-30pct': 'hsla(214, 50%, 22%, 0.26)',
      '--lumo-shade-40pct': 'hsla(214, 47%, 21%, 0.38)',
      '--lumo-shade-50pct': 'hsla(214, 45%, 20%, 0.5)',
      '--lumo-shade-5pct': 'hsla(214, 61%, 25%, 0.05)',
      '--lumo-shade-60pct': 'hsla(214, 43%, 19%, 0.61)',
      '--lumo-shade-70pct': 'hsla(214, 42%, 18%, 0.72)',
      '--lumo-shade-80pct': 'hsla(214, 41%, 17%, 0.83)',
      '--lumo-shade-90pct': 'hsla(214, 40%, 16%, 0.94)',
      '--lumo-success-color': 'hsl(145, 80%, 42%)',
      '--lumo-success-color-10pct': 'hsla(145, 76%, 44%, 0.12)',
      '--lumo-success-color-50pct': 'hsla(145, 76%, 44%, 0.55)',
      '--lumo-success-text-color': 'hsl(145, 100%, 32%)',
      '--lumo-tertiary-text-color': 'hsla(214, 45%, 20%, 0.5)',
      '--lumo-tint': '#FFF',
      '--lumo-tint-10pct': 'hsla(0, 0%, 100%, 0.37)',
      '--lumo-tint-20pct': 'hsla(0, 0%, 100%, 0.44)',
      '--lumo-tint-30pct': 'hsla(0, 0%, 100%, 0.5)',
      '--lumo-tint-40pct': 'hsla(0, 0%, 100%, 0.57)',
      '--lumo-tint-50pct': 'hsla(0, 0%, 100%, 0.64)',
      '--lumo-tint-5pct': 'hsla(0, 0%, 100%, 0.3)',
      '--lumo-tint-60pct': 'hsla(0, 0%, 100%, 0.7)',
      '--lumo-tint-70pct': 'hsla(0, 0%, 100%, 0.77)',
      '--lumo-tint-80pct': 'hsla(0, 0%, 100%, 0.84)',
      '--lumo-tint-90pct': 'hsla(0, 0%, 100%, 0.9)',
      '--lumo-base-color': '#FFF',
      '--lumo-body-text-color': 'hsla(214, 40%, 16%, 0.94)',
      '--lumo-border-radius': '0.25em',
      '--lumo-box-shadow-l': '0 3px 18px -2px  hsla(214, 53%, 23%, 0.16), 0 12px 48px -6px  hsla(214, 47%, 21%, 0.38)',
      '--lumo-box-shadow-m': '0 2px 6px -1px  hsla(214, 53%, 23%, 0.16), 0 8px 24px -4px  hsla(214, 47%, 21%, 0.38)',
      '--lumo-box-shadow-s': '0 1px 2px 0  hsla(214, 53%, 23%, 0.16), 0 2px 8px -2px  hsla(214, 47%, 21%, 0.38)',
      '--lumo-box-shadow-xl': '0 4px 24px -3px  hsla(214, 53%, 23%, 0.16), 0 18px 64px -8px  hsla(214, 47%, 21%, 0.38)',
      '--lumo-contrast': 'hsl(214, 35%, 15%)',
      '--lumo-contrast-10pct': 'hsla(214, 57%, 24%, 0.1)',
      '--lumo-contrast-20pct': 'hsla(214, 53%, 23%, 0.16)',
      '--lumo-contrast-30pct': 'hsla(214, 50%, 22%, 0.26)',
      '--lumo-contrast-40pct': 'hsla(214, 47%, 21%, 0.38)',
      '--lumo-contrast-50pct': 'hsla(214, 45%, 20%, 0.5)',
      '--lumo-contrast-5pct': 'hsla(214, 61%, 25%, 0.05)',
      '--lumo-contrast-60pct': 'hsla(214, 43%, 19%, 0.61)',
      '--lumo-contrast-70pct': 'hsla(214, 42%, 18%, 0.72)',
      '--lumo-contrast-80pct': 'hsla(214, 41%, 17%, 0.83)',
      '--lumo-contrast-90pct': 'hsla(214, 40%, 16%, 0.94)',
      '--lumo-disabled-text-color': 'hsla(214, 50%, 22%, 0.26)',
      '--lumo-error-color': 'hsl(3, 100%, 61%)',
      '--lumo-error-color-10pct': 'hsla(3, 100%, 60%, 0.1)',
      '--lumo-error-color-50pct': 'hsla(3, 100%, 60%, 0.5)',
      '--lumo-error-contrast-color': '#FFF',
      '--lumo-error-text-color': 'hsl(3, 92%, 53%)',
      '--lumo-font-family':
        '-apple-system, BlinkMacSystemFont, "Roboto", "Segoe UI", Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol"',
      '--lumo-font-size-l': '1.125rem',
      '--lumo-font-size-m': '1rem',
      '--lumo-font-size-s': '0.875rem',
      '--lumo-font-size-xl': '1.375rem',
      '--lumo-font-size-xs': '0.8125rem',
      '--lumo-font-size-xxl': '1.75rem',
      '--lumo-font-size-xxs': '0.75rem',
      '--lumo-font-size-xxxl': '2.5rem',
      '--lumo-header-text-color': 'hsl(214, 35%, 15%)',
      '--lumo-icon-size': '1.5em',
      '--lumo-icon-size-l': '2.25em',
      '--lumo-icon-size-m': '1.5em',
      '--lumo-icon-size-s': '1.25em',
      '--lumo-line-height-m': '1.625',
      '--lumo-line-height-s': '1.375',
      '--lumo-line-height-xs': '1.25',
      '--lumo-primary-color': 'hsl(214, 90%, 52%)',
      '--lumo-primary-color-10pct': 'hsla(214, 90%, 52%, 0.1)',
      '--lumo-primary-color-50pct': 'hsla(214, 90%, 52%, 0.5)',
      '--lumo-primary-contrast-color': '#FFF',
      '--lumo-primary-text-color': 'hsl(214, 90%, 52%)',
      '--lumo-secondary-text-color': 'hsla(214, 42%, 18%, 0.72)',
      '--lumo-shade': 'hsl(214, 35%, 15%)',
      '--lumo-shade-10pct': 'hsla(214, 57%, 24%, 0.1)',
      '--lumo-shade-20pct': 'hsla(214, 53%, 23%, 0.16)',
      '--lumo-shade-30pct': 'hsla(214, 50%, 22%, 0.26)',
      '--lumo-shade-40pct': 'hsla(214, 47%, 21%, 0.38)',
      '--lumo-shade-50pct': 'hsla(214, 45%, 20%, 0.5)',
      '--lumo-shade-5pct': 'hsla(214, 61%, 25%, 0.05)',
      '--lumo-shade-60pct': 'hsla(214, 43%, 19%, 0.61)',
      '--lumo-shade-70pct': 'hsla(214, 42%, 18%, 0.72)',
      '--lumo-shade-80pct': 'hsla(214, 41%, 17%, 0.83)',
      '--lumo-shade-90pct': 'hsla(214, 40%, 16%, 0.94)',
      '--lumo-size-l': '2.75rem',
      '--lumo-size-m': '2.25rem',
      '--lumo-size-s': '1.875rem',
      '--lumo-size-xl': '3.5rem',
      '--lumo-size-xs': '1.625rem',
      '--lumo-space-l': '1.5rem',
      '--lumo-space-m': '1rem',
      '--lumo-space-s': '0.5rem',
      '--lumo-space-tall-l': '1.5rem calc( 1.5rem / 2)',
      '--lumo-space-tall-m': '1rem calc( 1rem / 2)',
      '--lumo-space-tall-s': '0.5rem calc( 0.5rem / 2)',
      '--lumo-space-tall-xl': '2.5rem calc( 2.5rem / 2)',
      '--lumo-space-tall-xs': '0.25rem calc( 0.25rem / 2)',
      '--lumo-space-wide-l': 'calc( 1.5rem / 2)  1.5rem',
      '--lumo-space-wide-m': 'calc( 1rem / 2)  1rem',
      '--lumo-space-wide-s': 'calc( 0.5rem / 2)  0.5rem',
      '--lumo-space-wide-xl': 'calc( 2.5rem / 2)  2.5rem',
      '--lumo-space-wide-xs': 'calc( 0.25rem / 2)  0.25rem',
      '--lumo-space-xl': '2.5rem',
      '--lumo-space-xs': '0.25rem',
      '--lumo-success-color': 'hsl(145, 80%, 42%)',
      '--lumo-success-color-10pct': 'hsla(145, 76%, 44%, 0.12)',
      '--lumo-success-color-50pct': 'hsla(145, 76%, 44%, 0.55)',
      '--lumo-success-contrast-color': '#FFF',
      '--lumo-success-text-color': 'hsl(145, 100%, 32%)',
      '--lumo-tertiary-text-color': 'hsla(214, 45%, 20%, 0.5)',
      '--lumo-tint': '#FFF',
      '--lumo-tint-10pct': 'hsla(0, 0%, 100%, 0.37)',
      '--lumo-tint-20pct': 'hsla(0, 0%, 100%, 0.44)',
      '--lumo-tint-30pct': 'hsla(0, 0%, 100%, 0.5)',
      '--lumo-tint-40pct': 'hsla(0, 0%, 100%, 0.57)',
      '--lumo-tint-50pct': 'hsla(0, 0%, 100%, 0.64)',
      '--lumo-tint-5pct': 'hsla(0, 0%, 100%, 0.3)',
      '--lumo-tint-60pct': 'hsla(0, 0%, 100%, 0.7)',
      '--lumo-tint-70pct': 'hsla(0, 0%, 100%, 0.77)',
      '--lumo-tint-80pct': 'hsla(0, 0%, 100%, 0.84)',
      '--lumo-tint-90pct': 'hsla(0, 0%, 100%, 0.9)',
      '--lumo-border-radius-s': 'calc(var(--lumo-border-radius-m) / 2)',
      '--lumo-border-radius-m': 'var(--lumo-border-radius)',
      '--lumo-border-radius-l': 'calc(var(--lumo-border-radius-m) * 2)'
    };
  }

  ready() {
    super.ready();
    document.addEventListener(LumoEditor.PROPERTY_CHANGED, (e) => this._handlePropertyChange(e.detail));
    if (!this.previewDocument) {
      this.previewDocument = document;
    }
    document.addEventListener('keydown', (e) => {
      if (e.metaKey || e.ctrlKey) {
        if (e.key == 'z') {
          if (e.shiftKey) {
            this.redo();
          } else {
            this.undo();
          }
        } else if (e.key == 'y') {
          this.redo();
        }
      }
    });
  }

  _handlePropertyChange(entry) {
    if (!entry.history) {
      // Update from an editor (i.e. not from undo/redo)
      this._addHistoryEntry(entry);
    }

    // Update undo & redo button state
    this._updateButtonState();

    // Update model
    for (var prop in entry.properties) {
      if (entry.paletteMode) {
        if (entry.properties[prop]) {
          this.properties[entry.paletteMode][prop] = entry.properties[prop];
        } else {
          delete this.properties[entry.paletteMode][prop];
        }
      } else {
        if (entry.properties[prop]) {
          this.properties.global[prop] = entry.properties[prop];
        } else {
          delete this.properties.global[prop];
        }
      }
    }

    // Update preview
    this._updateGlobalStyleSheet();

    // Update file
    for (var property in entry.properties) {
      const value = entry.properties[property];

      this.dispatchEvent(
        new CustomEvent('css-property-updated', {
          detail: { property, value, paletteMode: entry.paletteMode },
          bubbles: true,
          composed: true
        })
      );
    }
  }

  _addHistoryEntry(entry) {
    this.historyIndex++;
    // Remove entries after this latest one (“future” entries)
    this.historyEntries = this.historyEntries.slice(0, this.historyIndex);
    // Mark the entry to be in history, so that it will not be added again on undo/redo
    entry.history = true;
    this.historyEntries[this.historyIndex] = entry;
  }

  undo() {
    if (this.historyIndex >= 0) {
      var entry = this.historyEntries[this.historyIndex--];
      this._undoEntry(entry);
    }
  }

  _undoEntry(currentEntry) {
    var diffEntry = {
      history: true,
      paletteMode: currentEntry.paletteMode,
      properties: {}
    };

    // Build a reversal entry by finding the previous value for each property in the current entry
    for (var currentEntryProp in currentEntry.properties) {
      // Assume no match is found
      diffEntry.properties[currentEntryProp] = undefined;

      // Start looping the history entries backwards to see if there's a match
      loop1: for (var i = this.historyEntries.length - 1; i >= 0; i--) {
        var historyEntry = this.historyEntries[i];

        // Look for a matching property in the history entry
        for (var prop in historyEntry.properties) {
          if (
            prop == currentEntryProp &&
            historyEntry.paletteMode == currentEntry.paletteMode &&
            i <= this.historyIndex
          ) {
            diffEntry.properties[prop] = historyEntry.properties[prop];
            break loop1;
          }
        }
      }
    }

    this._notifyPropertyChange(diffEntry);
  }

  redo() {
    if (this.historyIndex < this.historyEntries.length - 1) {
      var entry = this.historyEntries[++this.historyIndex];
      this._notifyPropertyChange(entry);
    }
  }

  reset() {
    this.historyIndex = -1;
    this.historyEntries = [];
    window.location.reload();
  }

  _confirmReset() {
    this.$.confirmReset.opened = true;
  }

  _updateButtonState() {
    this.shadowRoot.querySelector('.undo').disabled = this.historyIndex == -1;
    this.shadowRoot.querySelector('.redo').disabled = this.historyIndex == this.historyEntries.length - 1;
    this.shadowRoot.querySelector('.reset').disabled = this.historyIndex == -1;
  }

  _notifyPropertyChange(entry) {
    this.dispatchEvent(
      new CustomEvent(LumoEditor.PROPERTY_CHANGED, {
        detail: entry,
        bubbles: true,
        composed: true
      })
    );
  }

  _updateGlobalStyleSheet() {
    this._getGlobalStyleSheet().replaceSync(this._getStyleExport());
  }

  _getGlobalStyleSheet() {
    if (!this.lumoEditorSheet) {
      this.lumoEditorSheet = new CSSStyleSheet();
      this.lumoEditorSheet.replaceSync('');
      this.previewDocument.adoptedStyleSheets = [...this.previewDocument.adoptedStyleSheets, this.lumoEditorSheet];
    }
    return this.lumoEditorSheet;
  }

  _getStyleExport() {
    var rules = '';

    rules += `\nhtml {\n`;

    // Non-color properties
    for (var prop in this.properties.global) {
      if (prop.indexOf('--') == 0) {
        rules += `  ${prop}: ${this.properties.global[prop]};\n`;
      }
    }

    // Light palette properties
    for (var prop in this.properties.light) {
      if (prop.indexOf('--') == 0) {
        rules += `  ${prop}: ${this.properties.light[prop]};\n`;
      }
    }

    rules += '}\n';

    // Dark palette properties
    rules += `\n[theme~="dark"] {\n`;
    for (var prop in this.properties.dark) {
      if (prop.indexOf('--') == 0) {
        rules += `  ${prop}: ${this.properties.dark[prop]};\n`;
      }
    }

    rules += '}\n';

    return rules;
  }

  _getStyleModuleExport() {
    var modules = '';

    for (var prop in this.properties.global) {
      if (prop.indexOf('style-module') == 0) {
        var val = this.properties.global[prop];
        var component = prop.split(':')[1];

        modules += `\n\n<dom-module id="${component}-style" theme-for="vaadin-${component}">
<template>
<style>`;

        var styleModule = DomModule.prototype.modules[component + '--' + val];
        var css = styleModule.querySelector('template').content.querySelector('style').innerHTML;
        var regexp = new RegExp(`\\[theme~="${val}"\\]`, 'gi');

        modules += css.replace(regexp, '').replace(':host() ', '');

        modules += `\n    </style>\n  </template>\n</dom-module>`;
      }
    }

    return modules;
  }
}

customElements.define(LumoEditor.is, LumoEditor);

// Workaround to show all Vaadin component overlays on top of the dev tools window and adapt the same styles
registerStyles(
  'vaadin-*-overlay, vaadin-overlay',
  css`
    ${unsafeCSS(editorLumoPropertyOverrides)}

    :host([theme~="dev-tools-theme-editor"]) {
      z-index: 20001;
    }

    @supports (backdrop-filter: blur(1px)) {
      [part='overlay'] {
        backdrop-filter: blur(8px);
        background-color: var(--dev-tools-background-color-active-blurred);
      }
    }
  `
);
