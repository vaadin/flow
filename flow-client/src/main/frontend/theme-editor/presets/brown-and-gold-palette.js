import { PalettePreset } from './palette-preset.js';
class BrownAndGoldPalette extends PalettePreset {
  static get is() {
    return 'brown-and-gold-palette';
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
      '--lumo-primary-color': '#d8ab41',
      '--lumo-primary-text-color': '#b7830b',
      '--lumo-primary-contrast-color': undefined,
      '--lumo-error-color-50pct': undefined,
      '--lumo-error-color-10pct': undefined,
      '--lumo-error-color': '#ED7F97',
      '--lumo-error-text-color': '#d63d5e',
      '--lumo-error-contrast-color': undefined,
      '--lumo-success-color-50pct': undefined,
      '--lumo-success-color-10pct': undefined,
      '--lumo-success-color': '#54DE6E',
      '--lumo-success-text-color': '#2dae45',
      '--lumo-success-contrast-color': undefined,
      '--lumo-header-text-color': '#b7830b',
      '--lumo-body-text-color': undefined,
      '--lumo-secondary-text-color': undefined,
      '--lumo-tertiary-text-color': undefined,
      '--lumo-disabled-text-color': undefined
    };
  }
}

customElements.define(BrownAndGoldPalette.is, BrownAndGoldPalette);
