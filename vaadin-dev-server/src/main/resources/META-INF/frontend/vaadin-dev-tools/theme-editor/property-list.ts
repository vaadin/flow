import { css, html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ComponentMetadata, ComponentPartMetadata } from './metadata/model';
import './property-editor';
import { ComponentTheme } from './model';

@customElement('vaadin-dev-tools-theme-property-list')
export class PropertyList extends LitElement {
  static get styles() {
    return css`
      .part .header {
        padding: 0.4rem var(--theme-editor-section-horizontal-padding);
        color: var(--dev-tools-text-color-emphasis);
        background-color: rgba(0, 0, 0, 0.2);
      }

      .part .property-list .property-editor:not(:last-child) {
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }
    `;
  }

  @property({})
  public metadata!: ComponentMetadata;
  @property({})
  public theme!: ComponentTheme;

  render() {
    const partSections = this.metadata.parts.map((part) => this.renderPartSection(part));

    return html` <div class="part-list">${partSections}</div> `;
  }

  private renderPartSection(part: ComponentPartMetadata) {
    const properties = part.properties.map((property) => {
      return html` <vaadin-dev-tools-theme-property-editor
        class="property-editor"
        .partMetadata=${part}
        .propertyMetadata=${property}
        .theme=${this.theme}
      ></vaadin-dev-tools-theme-property-editor>`;
    });

    return html`
      <div class="part">
        <div class="header">${part.displayName}</div>
        <div class="property-list">${properties}</div>
      </div>
    `;
  }
}
