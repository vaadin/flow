import { css, html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ComponentPartMetadata, CssPropertyMetadata } from './metadata/model';
import { ComponentTheme } from './model';
import { ThemePropertyValueChangeEvent } from './events';

@customElement('vaadin-dev-tools-theme-property-editor')
export class PropertyEditor extends LitElement {
  @property({})
  public partMetadata!: ComponentPartMetadata;
  @property({})
  public propertyMetadata!: CssPropertyMetadata;
  @property({})
  public theme!: ComponentTheme;

  static get styles() {
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

      .property .property-editor {
        flex: 1 1 0;
      }

      .property .property-editor input {
        width: 100%;
      }

      .input {
        box-sizing: border-box;
        padding: 0.25rem 0.375rem;
        color: inherit;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 0.25rem;
        border: none;
      }
    `;
  }

  handleInputChange(e: Event) {
    const input = e.target as HTMLInputElement;
    this.dispatchEvent(new ThemePropertyValueChangeEvent(this.partMetadata, this.propertyMetadata, input.value));
  }

  render() {
    const propertyValue = this.theme.getPropertyValue(this.partMetadata.partName, this.propertyMetadata.propertyName);

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
