import { css, CSSResultGroup, html, LitElement, PropertyValues, TemplateResult } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { ComponentElementMetadata, CssPropertyMetadata } from '../../metadata/model';
import { ComponentTheme, ThemePropertyValue } from '../../model';
import { editorRowStyles } from '../../styles';
import { icons } from '../../icons';

export class ThemePropertyValueChangeEvent extends CustomEvent<{
  element: ComponentElementMetadata;
  property: CssPropertyMetadata;
  value: string;
}> {
  constructor(element: ComponentElementMetadata, property: CssPropertyMetadata, value: string) {
    super('theme-property-value-change', {
      bubbles: true,
      composed: true,
      detail: { element, property, value }
    });
  }
}

export abstract class BasePropertyEditor extends LitElement {
  static get styles(): CSSResultGroup {
    return [
      editorRowStyles,
      css`
        :host {
          display: block;
        }

        .editor-row .label .modified {
          display: inline-block;
          width: 6px;
          height: 6px;
          background: orange;
          border-radius: 3px;
          margin-left: 3px;
        }
      `
    ];
  }

  @property({})
  public elementMetadata!: ComponentElementMetadata;
  @property({})
  public propertyMetadata!: CssPropertyMetadata;
  @property({})
  public theme!: ComponentTheme;

  @state()
  protected propertyValue?: ThemePropertyValue;
  @state()
  protected value: string = '';

  protected update(changedProperties: PropertyValues) {
    super.update(changedProperties);

    if (changedProperties.has('propertyMetadata') || changedProperties.has('theme')) {
      this.updateValueFromTheme();
    }
  }

  render() {
    return html`
      <div class="editor-row">
        <div class="label">
          ${this.propertyMetadata.displayName}
          ${this.propertyValue?.modified ? html`<span class="modified"></span>` : null}
        </div>
        <div class="editor">${this.renderEditor()}</div>
      </div>
    `;
  }

  protected abstract renderEditor(): TemplateResult;

  protected updateValueFromTheme() {
    this.propertyValue = this.theme.getPropertyValue(this.elementMetadata.selector, this.propertyMetadata.propertyName);
    this.value = this.propertyValue?.value || '';
  }

  protected dispatchChange(value: string) {
    this.dispatchEvent(new ThemePropertyValueChangeEvent(this.elementMetadata, this.propertyMetadata, value));
  }
}

export class PropertyPresets {
  private _values: string[] = [];
  private _rawValues: { [key: string]: string } = {};

  get values(): string[] {
    return this._values;
  }

  get rawValues(): { [key: string]: string } {
    return this._rawValues;
  }

  constructor(propertyMetadata?: CssPropertyMetadata) {
    if (propertyMetadata) {
      const propertyName = propertyMetadata.propertyName;
      const presets = propertyMetadata.presets ?? [];

      // Convert presets that reference custom CSS properties in valid CSS
      // property values by wrapping them into var(...)
      this._values = (presets || []).map((preset) => (preset.startsWith('--') ? `var(${preset})` : preset));

      // Create map of presets to raw values. This allows to display `2.25rem`
      // instead of `--lumo-size-m` in the input field when changing the slider,
      // and in reverse allows to select a preset in the slider from a computed
      // style value such as `2.25rem`.
      const measureElement = document.createElement('div');
      // Enable borders so that measuring border styles works properly
      measureElement.style.borderStyle = 'solid';
      measureElement.style.visibility = 'hidden';
      document.body.append(measureElement);

      try {
        this._values.forEach((preset) => {
          measureElement.style.setProperty(propertyName, preset);
          const style = getComputedStyle(measureElement);
          this._rawValues[preset] = style.getPropertyValue(propertyName).trim();
        });
      } finally {
        measureElement.remove();
      }
    }
  }

  tryMapToRawValue(presetOrValue: string) {
    return this._rawValues[presetOrValue] ?? presetOrValue;
  }

  tryMapToPreset(value: string) {
    return this.findPreset(value) ?? value;
  }

  findPreset(rawValue: string) {
    const sanitizedValue = rawValue ? rawValue.trim() : rawValue;
    return this.values.find((preset) => this._rawValues[preset] === sanitizedValue);
  }
}

export class TextInputChangeEvent extends CustomEvent<{ value: string }> {
  constructor(value: string) {
    super('change', { detail: { value } });
  }
}

@customElement('vaadin-dev-tools-theme-text-input')
export class TextInput extends LitElement {
  static get styles() {
    return css`
      :host {
        display: inline-block;
        width: 100%;
        position: relative;
      }

      input {
        width: 100%;
        box-sizing: border-box;
        padding: 0.25rem 0.375rem;
        color: inherit;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 0.25rem;
        border: none;
      }

      button {
        display: none;
        position: absolute;
        right: 4px;
        top: 4px;
        padding: 0;
        line-height: 0;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
      }

      button svg {
        width: 16px;
        height: 16px;
      }

      button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      :host(.show-clear-button) input {
        padding-right: 20px;
      }

      :host(.show-clear-button) button {
        display: block;
      }
    `;
  }

  @property({})
  public value: string = '';
  @property({})
  public showClearButton: boolean = false;

  protected update(changedProperties: PropertyValues) {
    super.update(changedProperties);

    if (changedProperties.has('showClearButton')) {
      if (this.showClearButton) {
        this.classList.add('show-clear-button');
      } else {
        this.classList.remove('show-clear-button');
      }
    }
  }

  render() {
    return html`
      <input class="input" .value=${this.value} @change=${this.handleInputChange} />
      <button @click=${this.handleClearClick}>${icons.cross}</button>
    `;
  }

  private handleInputChange(e: Event) {
    const input = e.target as HTMLInputElement;

    this.dispatchEvent(new TextInputChangeEvent(input.value));
  }

  private handleClearClick() {
    this.dispatchEvent(new TextInputChangeEvent(''));
  }
}
