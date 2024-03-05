import { css, html, PropertyValues, TemplateResult } from 'lit';
import { customElement } from 'lit/decorators.js';
import './color-picker';
import { ColorPickerChangeEvent } from './color-picker';
import { BasePropertyEditor, PropertyPresets, TextInputChangeEvent } from './base-property-editor';

@customElement('vaadin-dev-tools-theme-color-property-editor')
export class ColorPropertyEditor extends BasePropertyEditor {
  static get styles() {
    return [
      BasePropertyEditor.styles,
      css`
        .editor-row {
          align-items: center;
        }

        .editor-row > .editor {
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }
      `
    ];
  }

  private presets: PropertyPresets = new PropertyPresets();

  protected update(changedProperties: PropertyValues) {
    if (changedProperties.has('propertyMetadata')) {
      this.presets = new PropertyPresets(this.propertyMetadata);
    }

    super.update(changedProperties);
  }

  protected renderEditor(): TemplateResult {
    return html`
      <vaadin-dev-tools-color-picker
        .value=${this.value}
        .presets=${this.presets.values}
        @color-picker-change=${this.handleColorPickerChange}
        @color-picker-commit=${this.handleColorPickerCommit}
        @color-picker-cancel=${this.handleColorPickerCancel}
      ></vaadin-dev-tools-color-picker>
      <vaadin-dev-tools-theme-text-input
        .value=${this.value}
        .showClearButton=${this.propertyValue?.modified || false}
        @change=${this.handleInputChange}
      ></vaadin-dev-tools-theme-text-input>
    `;
  }

  private handleInputChange(e: TextInputChangeEvent) {
    this.value = e.detail.value;
    this.dispatchChange(this.value);
  }

  private handleColorPickerChange(e: ColorPickerChangeEvent) {
    this.value = e.detail.value;
  }

  private handleColorPickerCommit() {
    this.dispatchChange(this.value);
  }

  private handleColorPickerCancel() {
    this.updateValueFromTheme();
  }

  protected dispatchChange(value: string) {
    // If value matches a raw preset value, send preset instead
    const effectiveValue = this.presets.tryMapToPreset(value);

    super.dispatchChange(effectiveValue);
  }

  protected updateValueFromTheme() {
    super.updateValueFromTheme();

    // If value matches a preset, then display raw preset value
    this.value = this.presets.tryMapToRawValue(this.propertyValue?.value || '');
  }
}
