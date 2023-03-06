import { html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ComponentPartMetadata, CssPropertyMetadata } from '../metadata/model';
import { ComponentTheme } from '../model';
import { ThemePropertyValueChangeEvent } from '../events';
import { sharedStyles } from './shared';

@customElement('vaadin-dev-tools-theme-text-property-editor')
export class TextPropertyEditor extends LitElement {
  static get styles() {
    return [sharedStyles];
  }

  @property({})
  public partMetadata?: ComponentPartMetadata;
  @property({})
  public propertyMetadata!: CssPropertyMetadata;
  @property({})
  public theme!: ComponentTheme;

  handleInputChange(e: Event) {
    const input = e.target as HTMLInputElement;
    this.dispatchEvent(
      new ThemePropertyValueChangeEvent(this.partMetadata || null, this.propertyMetadata, input.value)
    );
  }

  render() {
    const partName = this.partMetadata?.partName || null;
    const propertyValue = this.theme.getPropertyValue(partName, this.propertyMetadata.propertyName);

    return html`
      <div class="property">
        <div class="property-name">${this.propertyMetadata.displayName}</div>
        <div class="property-editor">
          <input class="input" .value=${propertyValue.value} @change=${this.handleInputChange} />
        </div>
      </div>
    `;
  }
}
