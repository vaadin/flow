import { css, html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ComponentMetadata, ComponentPartMetadata, CssPropertyMetadata } from './metadata/model';
import './property-editor';
import { ComponentTheme } from './model';

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
    const propertiesList = properties.map((property) => {
      return html` <vaadin-dev-tools-theme-property-editor
        class="property-editor"
        .partMetadata=${part}
        .propertyMetadata=${property}
        .theme=${this.theme}
        data-testid=${property.propertyName}
      ></vaadin-dev-tools-theme-property-editor>`;
    });

    return html`
      <div class="section" data-testid=${part?.partName || 'host'}>
        ${part ? html` <div class="header">${part.displayName}</div>` : null}
        <div class="property-list">${propertiesList}</div>
      </div>
    `;
  }
}
