import { css, CSSResultGroup, html, LitElement, PropertyValues, TemplateResult } from 'lit';
import { property, state } from 'lit/decorators.js';
import { ComponentPartMetadata, CssPropertyMetadata } from '../metadata/model';
import { ComponentTheme, ThemePropertyValue } from '../model';

export class ThemePropertyValueChangeEvent extends CustomEvent<{
  part: ComponentPartMetadata | null;
  property: CssPropertyMetadata;
  value: string;
}> {
  constructor(part: ComponentPartMetadata | null, property: CssPropertyMetadata, value: string) {
    super('theme-property-value-change', {
      bubbles: true,
      composed: true,
      detail: { part, property, value }
    });
  }
}

export abstract class BasePropertyEditor extends LitElement {
  static get styles(): CSSResultGroup {
    return css`
      :host {
        display: block;
      }

      .property {
        display: flex;
        align-items: baseline;
        padding: var(--theme-editor-section-horizontal-padding);
      }

      .property .property-name {
        flex: 0 0 auto;
        width: 100px;
      }

      .property .property-name .modified {
        display: inline-block;
        width: 6px;
        height: 6px;
        background: orange;
        border-radius: 3px;
        margin-left: 3px;
      }

      .property .property-editor {
        flex: 1 1 0;
      }

      .input {
        width: 100%;
        box-sizing: border-box;
        padding: 0.25rem 0.375rem;
        color: inherit;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 0.25rem;
        border: none;
      }
    `;
  }

  @property({})
  public partMetadata?: ComponentPartMetadata;
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
      <div class="property">
        <div class="property-name">
          ${this.propertyMetadata.displayName}
          ${this.propertyValue?.modified ? html`<span class="modified"></span>` : null}
        </div>
        <div class="property-editor">${this.renderEditor()}</div>
      </div>
    `;
  }

  protected abstract renderEditor(): TemplateResult;

  protected updateValueFromTheme() {
    const partName = this.partMetadata?.partName || null;
    this.propertyValue = this.theme.getPropertyValue(partName, this.propertyMetadata.propertyName);
    this.value = this.propertyValue?.value || '';
  }

  protected dispatchChange(value: string) {
    this.dispatchEvent(new ThemePropertyValueChangeEvent(this.partMetadata || null, this.propertyMetadata, value));
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
