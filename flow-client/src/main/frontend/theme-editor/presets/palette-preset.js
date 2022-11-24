import { PresetItem } from '../modules/preset-item.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
export class PalettePreset extends PresetItem {
  static get template() {
    return html`
    <style include="preset-item lumo-item">
      :host {
        display: flex;
        flex: auto;
        padding: 2px;
        margin: 4px 0;

        /* TODO not ideal, but this way we can show the default colors */
        --lumo-tint: #fff;
        --lumo-shade: hsl(214, 35%, 15%);
        --lumo-primary-color: hsl(214, 90%, 52%);
        --lumo-primary-contrast-color: #fff;
        --lumo-primary-text-color: var(--lumo-primary-color);
        --lumo-error-color: hsl(3, 100%, 61%);
        --lumo-error-text-color: hsl(3, 92%, 53%);
        --lumo-error-contrast-color: #fff;
        --lumo-success-color: hsl(145, 80%, 42%);
        --lumo-success-text-color: hsl(145, 100%, 32%);
        --lumo-success-contrast-color: #fff;
      }

      .base {
        flex: auto;
        background-color: var(--lumo-base-color);
        display: flex;
        align-items: stretch;
        height: 24px;
        padding: 6px;
        border-radius: var(--lumo-border-radius);
      }

      .base > * {
        flex: 1;
      }

      .base > :first-child {
        border-top-left-radius: inherit;
        border-bottom-left-radius: inherit;
      }

      .base > :last-child {
        border-top-right-radius: inherit;
        border-bottom-right-radius: inherit;
      }

      .primary {
        background-color: var(--lumo-primary-color);
        flex: 2;
      }

      .error {
        background-color: var(--lumo-error-color);
      }

      .success {
        background-color: var(--lumo-success-color);
      }

      .shade {
        background-color: var(--lumo-shade);
        box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1);
        flex: 0.5;
      }

      .tint {
        background-color: var(--lumo-tint);
        box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.2);
        flex: 0.5;
      }

      .name {
        font-weight: 500;
        font-size: var(--lumo-font-size-s);
        color: var(--lumo-secondary-text-color);
        padding: 4px 8px;
      }
    </style>
    <div class="base">
      <div class="shade"></div>
      <div class="tint"></div>
      <div class="primary"></div>
      <div class="error"></div>
      <div class="success"></div>
    </div>
`;
  }

  static get is() {
    return 'palette-preset';
  }

  ready() {
    this.preview = true;
    super.ready();
  }
}

customElements.define(PalettePreset.is, PalettePreset);
