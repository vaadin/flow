import { css, html, PropertyValues, TemplateResult } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { classMap } from 'lit/directives/class-map.js';
//import { icons } from '../../icons';
import { BasePropertyEditor, PropertyPresets, TextInputChangeEvent } from './base-property-editor';

@customElement('vaadin-dev-tools-theme-range-property-editor')
export class RangePropertyEditor extends BasePropertyEditor {
  static get styles() {
    return [
      BasePropertyEditor.styles,
      css`
        :host {
          --preset-count: 3;
          --slider-bg: #fff;
          --slider-border: #333;
        }

        .editor-row {
          align-items: center;
        }

        .editor-row > .editor {
          display: flex;
          align-items: center;
          gap: 1rem;
        }

        .editor-row .input {
          flex: 0 0 auto;
          width: 80px;
        }

        .slider-wrapper {
          flex: 1 1 0;
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .icon {
          width: 20px;
          height: 20px;
          color: #aaa;
        }

        .icon.prefix > svg {
          transform: scale(0.75);
        }

        .slider {
          flex: 1 1 0;
          -webkit-appearance: none;
          background: linear-gradient(to right, #666, #666 2px, transparent 2px);
          background-size: calc((100% - 13px) / (var(--preset-count) - 1)) 8px;
          background-position: 5px 50%;
          background-repeat: repeat-x;
        }

        .slider::-webkit-slider-runnable-track {
          width: 100%;
          box-sizing: border-box;
          height: 16px;
          background-image: linear-gradient(#666, #666);
          background-size: calc(100% - 12px) 2px;
          background-repeat: no-repeat;
          background-position: 6px 50%;
        }

        .slider::-moz-range-track {
          width: 100%;
          box-sizing: border-box;
          height: 16px;
          background-image: linear-gradient(#666, #666);
          background-size: calc(100% - 12px) 2px;
          background-repeat: no-repeat;
          background-position: 6px 50%;
        }

        .slider::-webkit-slider-thumb {
          -webkit-appearance: none;
          height: 16px;
          width: 16px;
          border: 2px solid var(--slider-border);
          border-radius: 50%;
          background: var(--slider-bg);
          cursor: pointer;
        }

        .slider::-moz-range-thumb {
          height: 16px;
          width: 16px;
          border: 2px solid var(--slider-border);
          border-radius: 50%;
          background: var(--slider-bg);
          cursor: pointer;
        }

        .custom-value {
          opacity: 0.5;
        }

        .custom-value:hover,
        .custom-value:focus-within {
          opacity: 1;
        }

        .custom-value:not(:hover, :focus-within) {
          --slider-bg: #333;
          --slider-border: #666;
        }
      `
    ];
  }

  @state()
  private selectedPresetIndex: number = -1;
  @state()
  private presets: PropertyPresets = new PropertyPresets();

  protected update(changedProperties: PropertyValues) {
    if (changedProperties.has('propertyMetadata')) {
      this.presets = new PropertyPresets(this.propertyMetadata);
    }

    super.update(changedProperties);
  }

  protected renderEditor(): TemplateResult {
    const sliderWrapperClasses = {
      'slider-wrapper': true,
      'custom-value': this.selectedPresetIndex < 0
    };
    //const iconName = this.propertyMetadata.icon;
    //const icon = iconName ? (icons as any)[iconName] : null;
    const presetCount = this.presets.values.length;

    return html`
      <div class=${classMap(sliderWrapperClasses)}>
        ${null /*icon ? html` <div class="icon prefix">${icon}</div>` : null*/}
        <input
          type="range"
          class="slider"
          style="--preset-count: ${presetCount}"
          step="1"
          min="0"
          .max=${(presetCount - 1).toString()}
          .value=${this.selectedPresetIndex}
          @input=${this.handleSliderInput}
          @change=${this.handleSliderChange}
        />
        ${null /*icon ? html` <div class="icon suffix">${icon}</div>` : null*/}
      </div>
      <vaadin-dev-tools-theme-text-input
        class="input"
        .value=${this.value}
        .showClearButton=${this.propertyValue?.modified || false}
        @change=${this.handleValueChange}
      ></vaadin-dev-tools-theme-text-input>
    `;
  }

  private handleSliderInput(e: Event) {
    const input = e.target as HTMLInputElement;
    const selectedIndex = parseInt(input.value);
    const preset = this.presets.values[selectedIndex];

    this.selectedPresetIndex = selectedIndex;
    this.value = this.presets.rawValues[preset];
  }

  private handleSliderChange() {
    this.dispatchChange(this.value);
  }

  private handleValueChange(e: TextInputChangeEvent) {
    this.value = e.detail.value;

    this.updateSliderValue();
    this.dispatchChange(this.value);
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
    this.updateSliderValue();
  }

  private updateSliderValue() {
    const maybePreset = this.presets.findPreset(this.value);
    this.selectedPresetIndex = maybePreset ? this.presets.values.indexOf(maybePreset) : -1;
  }
}
