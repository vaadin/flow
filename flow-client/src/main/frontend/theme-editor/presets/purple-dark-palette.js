import { PalettePreset } from './palette-preset.js';
class PurpleDarkPalette extends PalettePreset {
  static get is() {
    return 'purple-dark-palette';
  }

  ready() {
    super.ready();
    this.properties = {
      '--lumo-base-color': 'hsl(255, 35%, 23%)',
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
      '--lumo-tint': 'hsl(285, 100%, 98%)',
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
      '--lumo-shade': 'hsl(249, 46%, 4%)',
      '--lumo-primary-color-50pct': undefined,
      '--lumo-primary-color-10pct': undefined,
      '--lumo-primary-color': 'hsl(265, 86%, 55%)',
      '--lumo-primary-text-color': 'rgb(178, 122, 255)',
      '--lumo-primary-contrast-color': undefined,
      '--lumo-error-color-50pct': undefined,
      '--lumo-error-color-10pct': undefined,
      '--lumo-error-color': 'hsl(346, 100%, 61%)',
      '--lumo-error-text-color': 'hsl(346, 100%, 67%)',
      '--lumo-error-contrast-color': undefined,
      '--lumo-success-color-50pct': undefined,
      '--lumo-success-color-10pct': undefined,
      '--lumo-success-color': 'hsl(165, 80%, 40%)',
      '--lumo-success-text-color': 'hsl(165, 85%, 47%)',
      '--lumo-header-text-color': 'hsl(285, 100%, 98%)',
      '--lumo-body-text-color': 'hsla(285, 96%, 96%, 0.9)',
      '--lumo-secondary-text-color': 'hsla(285, 87%, 92%, 0.7)',
      '--lumo-tertiary-text-color': 'hsla(285, 78%, 88%, 0.5)',
      '--lumo-disabled-text-color': 'hsla(285, 69%, 84%, 0.32)'
    };
  }
}

customElements.define(PurpleDarkPalette.is, PurpleDarkPalette);
