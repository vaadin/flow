import { PalettePreset } from './palette-preset.js';
const $_documentContainer = document.createElement('template');

document.head.appendChild($_documentContainer.content);
class DefaultPalette extends PalettePreset {
  static get is() {
    return 'default-palette';
  }

  ready() {
    super.ready();
    this.properties = {
      '--lumo-base-color': undefined,
      '--lumo-tint': undefined,
      '--lumo-tint-90pct': undefined,
      '--lumo-tint-80pct': undefined,
      '--lumo-tint-70pct': undefined,
      '--lumo-tint-60pct': undefined,
      '--lumo-tint-50pct': undefined,
      '--lumo-tint-40pct': undefined,
      '--lumo-tint-30pct': undefined,
      '--lumo-tint-20pct': undefined,
      '--lumo-tint-10pct': undefined,
      '--lumo-tint-5pct': undefined,
      '--lumo-shade': undefined,
      '--lumo-shade-90pct': undefined,
      '--lumo-shade-80pct': undefined,
      '--lumo-shade-70pct': undefined,
      '--lumo-shade-60pct': undefined,
      '--lumo-shade-50pct': undefined,
      '--lumo-shade-40pct': undefined,
      '--lumo-shade-30pct': undefined,
      '--lumo-shade-20pct': undefined,
      '--lumo-shade-10pct': undefined,
      '--lumo-shade-5pct': undefined,
      '--lumo-primary-color': undefined,
      '--lumo-primary-color-50pct': undefined,
      '--lumo-primary-color-10pct': undefined,
      '--lumo-primary-contrast-color': undefined,
      '--lumo-primary-text-color': undefined,
      '--lumo-error-color': undefined,
      '--lumo-error-color-50pct': undefined,
      '--lumo-error-color-10pct': undefined,
      '--lumo-error-contrast-color': undefined,
      '--lumo-error-text-color': undefined,
      '--lumo-success-color': undefined,
      '--lumo-success-color-50pct': undefined,
      '--lumo-success-color-10pct': undefined,
      '--lumo-success-contrast-color': undefined,
      '--lumo-success-text-color': undefined,
      '--lumo-header-text-color': undefined,
      '--lumo-body-text-color': undefined,
      '--lumo-secondary-text-color': undefined,
      '--lumo-tertiary-text-color': undefined,
      '--lumo-disabled-text-color': undefined
    };
  }
}

customElements.define(DefaultPalette.is, DefaultPalette);
