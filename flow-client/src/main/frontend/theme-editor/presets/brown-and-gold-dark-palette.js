import { PalettePreset } from "./palette-preset.js";
class BrownAndGoldDarkPalette extends PalettePreset {
  static get is() {
    return "brown-and-gold-dark-palette";
  }

  ready() {
    super.ready();
    this.properties = {
      '--lumo-base-color': '#2D2C29',
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
      '--lumo-tint': '#f4eee1',
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
      '--lumo-shade': 'hsl(42, 33%, 13%)',
      '--lumo-primary-color-50pct': undefined,
      '--lumo-primary-color-10pct': undefined,
      '--lumo-primary-color': '#E6BE5F',
      '--lumo-primary-contrast-color': '#2D2C29',
      '--lumo-primary-text-color': '#E6BE5F',
      '--lumo-error-color-50pct': undefined,
      '--lumo-error-color-10pct': undefined,
      '--lumo-error-color': '#ED7F97',
      '--lumo-error-text-color': '#ED7F97',
      '--lumo-error-contrast-color': '#2D2C29',
      '--lumo-success-color-50pct': undefined,
      '--lumo-success-color-10pct': undefined,
      '--lumo-success-color': '#54DE6E',
      '--lumo-success-text-color': '#54DE6E',
      '--lumo-success-contrast-color': '#2D2C29',
      '--lumo-header-text-color': '#E6BE5F',
      '--lumo-body-text-color': 'hsla(53, 96%, 96%, 0.9)',
      '--lumo-secondary-text-color': 'hsla(42, 73%, 64%, 0.7)',
      '--lumo-tertiary-text-color': 'hsla(42, 73%, 64%, 0.5)',
      '--lumo-disabled-text-color': 'hsla(42, 73%, 64%, 0.32)'
    };
  }
}

customElements.define(BrownAndGoldDarkPalette.is, BrownAndGoldDarkPalette);
