import { css, html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { html as staticHtml, literal, StaticValue } from 'lit/static-html.js';
import { ComponentMetadata, ComponentPartMetadata, CssPropertyMetadata, EditorType } from './metadata/model';
import { ComponentTheme } from './model';
import './editors/text-property-editor';
import './editors/range-property-editor';

@customElement('vaadin-dev-tools-theme-property-list')
export class PropertyList extends LitElement {
  static get styles() {
    return css`
      .section .header {
        padding: 0.4rem var(--theme-editor-section-horizontal-padding);
        color: var(--dev-tools-text-color-emphasis);
        background-color: rgba(0, 0, 0, 0.2);
      }

      .section .property-list .property-editor:not(:last-child) {
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }
    `;
  }

  @property({})
  public metadata!: ComponentMetadata;
  @property({})
  public theme!: ComponentTheme;

  render() {
    const sections = [
      this.renderSection(null, this.metadata.properties),
      ...this.metadata.parts.map((part) => this.renderSection(part, part.properties))
    ];

    return html` <div>${sections}</div> `;
  }

  private renderSection(part: ComponentPartMetadata | null, properties: CssPropertyMetadata[]) {
    const propertiesList = properties.map((property) => this.renderPropertyEditor(part, property));

    return html`
      <div class="section" data-testid=${part?.partName || 'host'}>
        ${part ? html` <div class="header">${part.displayName}</div>` : null}
        <div class="property-list">${propertiesList}</div>
      </div>
    `;
  }

  private renderPropertyEditor(part: ComponentPartMetadata | null, property: CssPropertyMetadata) {
    let editorTagName: StaticValue;
    switch (property.editorType) {
      case EditorType.range:
        editorTagName = literal`vaadin-dev-tools-theme-range-property-editor`;
        break;
      default:
        editorTagName = literal`vaadin-dev-tools-theme-text-property-editor`;
    }

    return staticHtml` <${editorTagName}
          class="property-editor"
          .partMetadata=${part}
          .propertyMetadata=${property}
          .theme=${this.theme}
          data-testid=${property.propertyName}
        >
        </${editorTagName}>`;
  }
}
