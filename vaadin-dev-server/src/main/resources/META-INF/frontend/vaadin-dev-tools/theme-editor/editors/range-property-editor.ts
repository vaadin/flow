import { css, html, LitElement, PropertyValues } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { classMap } from 'lit/directives/class-map.js';
import { ComponentPartMetadata, CssPropertyMetadata } from '../metadata/model';
import { ComponentTheme } from '../model';
import { ThemePropertyValueChangeEvent } from '../events';
import { icons } from '../icons';
import { sharedStyles } from './shared';

@customElement('vaadin-dev-tools-theme-range-property-editor')
export class RangePropertyEditor extends LitElement {
  static get styles() {
    return [
      sharedStyles,
      css`
        :host {
          --preset-count: 3;
          --slider-bg: #fff;
          --slider-border: #333;
        }

        .property {
          align-items: center;
        }

        .property .property-editor {
          display: flex;
          align-items: center;
          gap: 1rem;
        }

        .input {
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
          -webkit-appearance: none;
          background: linear-gradient(to right, #666, #666 2px, transparent 2px);
          background-size: calc((100% - 13px) / (var(--preset-count) - 1)) 8px;
          background-position: 5px 50%;
          background-repeat: repeat-x;
          width: 15em;
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

  @property({})
  public partMetadata?: ComponentPartMetadata;
  @property({})
  public propertyMetadata!: CssPropertyMetadata;
  @property({})
  public theme!: ComponentTheme;

  @state()
  value: string = '';
  @state()
  modified?: boolean;
  @state()
  selectedPresetIndex: number = -1;
  @state()
  private presets: string[] = [];
  private rawPresetValues: { [key: string]: string } = {};

  protected update(changedProperties: PropertyValues) {
    super.update(changedProperties);

    if (changedProperties.has('propertyMetadata')) {
      this.measurePresetValues();
    }

    if (changedProperties.has('propertyMetadata') || changedProperties.has('theme')) {
      this.updateValueFromTheme();
    }
  }

  render() {
    const sliderWrapperClasses = {
      'slider-wrapper': true,
      'custom-value': this.selectedPresetIndex < 0
    };
    const iconName = this.propertyMetadata.icon;
    const icon = iconName ? (icons as any)[iconName] : null;

    return html`
      <div class="property">
        <div class="property-name">
          ${this.propertyMetadata.displayName} ${this.modified ? html`<span class="modified"></span>` : null}
        </div>
        <div class="property-editor">
          <div class=${classMap(sliderWrapperClasses)}>
            ${icon ? html` <div class="icon prefix">${icon}</div>` : null}
            <input
              type="range"
              class="slider"
              style="--preset-count: ${this.presets.length}"
              step="1"
              min="0"
              .max=${(this.presets.length - 1).toString()}
              .value=${this.selectedPresetIndex}
              @input=${this.handleSliderInput}
              @change=${this.handleSliderChange}
            />
            ${icon ? html` <div class="icon suffix">${icon}</div>` : null}
          </div>
          <input type="text" class="input" .value=${this.value} @change=${this.handleValueChange} />
        </div>
      </div>
    `;
  }

  private measurePresetValues() {
    if (!this.propertyMetadata || !this.theme) {
      return;
    }

    // Convert presets that reference custom CSS properties in valid CSS
    // property values by wrapping them into var(...)
    this.presets = (this.propertyMetadata.presets || []).map((preset) =>
      preset.startsWith('--') ? `var(${preset})` : preset
    );

    // Create map of presets to raw values. This allows to display `2.25rem`
    // instead of `--lumo-size-m` in the input field when changing the slider,
    // and in reverse allows to select a preset in the slider from a computed
    // style value such as `2.25rem`.
    this.rawPresetValues = {};
    const measureElement = document.createElement('div');
    measureElement.style.visibility = 'hidden';
    document.body.append(measureElement);

    try {
      this.presets.forEach((preset) => {
        measureElement.style.setProperty(this.propertyMetadata.propertyName, preset);
        const style = getComputedStyle(measureElement);
        this.rawPresetValues[preset] = style.getPropertyValue(this.propertyMetadata.propertyName).trim();
      });
    } finally {
      measureElement.remove();
    }
  }

  private handleSliderInput(e: Event) {
    const input = e.target as HTMLInputElement;
    const selectedIndex = parseInt(input.value);
    const preset = this.presets[selectedIndex];

    this.selectedPresetIndex = selectedIndex;
    this.value = this.rawPresetValues[preset];
  }

  private handleSliderChange() {
    this.dispatchChange();
  }

  private handleValueChange(e: Event) {
    const input = e.target as HTMLInputElement;
    this.value = input.value;

    this.updateSliderValue();
    this.dispatchChange();
  }

  private dispatchChange() {
    // If value matches a raw preset value, send preset instead
    const preset = this.findPreset(this.value);
    const effectiveValue = preset || this.value;

    this.dispatchEvent(
      new ThemePropertyValueChangeEvent(this.partMetadata || null, this.propertyMetadata, effectiveValue)
    );
  }

  private updateValueFromTheme() {
    const partName = this.partMetadata?.partName || null;
    const propertyValue = this.theme.getPropertyValue(partName, this.propertyMetadata.propertyName);

    // If value matches a preset, then display raw preset value
    this.value = this.rawPresetValues[propertyValue.value] || propertyValue.value;
    this.modified = propertyValue.modified;
    this.updateSliderValue();
  }

  private updateSliderValue() {
    const maybePreset = this.findPreset(this.value);
    this.selectedPresetIndex = maybePreset ? this.presets.indexOf(maybePreset) : -1;
  }

  private findPreset(rawValue: string) {
    const sanitizedValue = rawValue ? rawValue.trim() : rawValue;
    return this.presets.find((preset) => this.rawPresetValues[preset] === sanitizedValue);
  }
}
