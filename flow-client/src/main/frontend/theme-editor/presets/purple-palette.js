import { PalettePreset } from './palette-preset.js';
class PurplePalette extends PalettePreset {
  static get is() {
    return 'purple-palette';
  }

  ready() {
    super.ready();
    this.properties = {
      '--lumo-base-color': undefined,
      '--lumo-tint-5pct': undefined,
      '--lumo-tint-10pct': undefined,
      '--lumo-tint-20pct': undefined,
      '--lumo-tint-30pct': undefined,
      '--lumo-tint-40pct': undefined,
      '--lumo-tint-50pct': undefined,
      '--lumo-tint-60pct': undefined,
      '--lumo-tint-70pct': undefined,
      '--lumo-tint-80pct': undefined,
      '--lumo-tint-90pct': undefined,
      '--lumo-tint': undefined,
      '--lumo-shade-5pct': undefined,
      '--lumo-shade-10pct': undefined,
      '--lumo-shade-20pct': undefined,
      '--lumo-shade-30pct': undefined,
      '--lumo-shade-40pct': undefined,
      '--lumo-shade-50pct': undefined,
      '--lumo-shade-60pct': undefined,
      '--lumo-shade-70pct': undefined,
      '--lumo-shade-80pct': undefined,
      '--lumo-shade-90pct': undefined,
      '--lumo-shade': undefined,
      '--lumo-primary-color-50pct': undefined,
      '--lumo-primary-color-10pct': undefined,
      '--lumo-primary-color': 'hsl(265, 90%, 52%)',
      '--lumo-primary-text-color': undefined,
      '--lumo-primary-contrast-color': undefined,
      '--lumo-error-color-50pct': undefined,
      '--lumo-error-color-10pct': undefined,
      '--lumo-error-color': 'hsl(346, 100%, 61%)',
      '--lumo-error-text-color': 'hsl(346, 92%, 53%)',
      '--lumo-error-contrast-color': undefined,
      '--lumo-success-color-50pct': undefined,
      '--lumo-success-color-10pct': undefined,
      '--lumo-success-color': 'hsl(165, 80%, 40%)',
      '--lumo-success-text-color': 'hsl(165, 100%, 32%)',
      '--lumo-success-contrast-color': undefined,
      '--lumo-header-text-color': 'hsl(285, 35%, 15%)',
      '--lumo-body-text-color': 'hsla(285, 40%, 16%, 0.94)',
      '--lumo-secondary-text-color': 'hsla(285, 42%, 18%, 0.72)',
      '--lumo-tertiary-text-color': 'hsla(285, 45%, 20%, 0.5)',
      '--lumo-disabled-text-color': 'hsla(285, 50%, 22%, 0.26)'
    };
  }
}

customElements.define(PurplePalette.is, PurplePalette);
