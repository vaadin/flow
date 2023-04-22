import { css, html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { html as staticHtml, literal, StaticValue } from 'lit/static-html.js';
import { ComponentElementMetadata, ComponentMetadata, CssPropertyMetadata, EditorType } from '../metadata/model';
import { ComponentTheme } from '../model';
import './editors/checkbox-property-editor';
import './editors/text-property-editor';
import './editors/range-property-editor';
import './editors/color-property-editor';

export class OpenCssEvent extends CustomEvent<{ element: ComponentElementMetadata }> {
  constructor(element: ComponentElementMetadata) {
    super('open-css', { detail: { element } });
  }
}

@customElement('vaadin-dev-tools-theme-property-list')
export class PropertyList extends LitElement {
  static get styles() {
    return css`
      .section .header {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        padding: 0.4rem var(--theme-editor-section-horizontal-padding);
        color: var(--dev-tools-text-color-emphasis);
        background-color: rgba(0, 0, 0, 0.2);
      }

      .section .property-list .property-editor:not(:last-child) {
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }

      .section .header .open-css {
        all: initial;
        font-family: inherit;
        font-size: var(--dev-tools-font-size-small);
        line-height: 1;
        white-space: nowrap;
        background-color: rgba(255, 255, 255, 0.12);
        color: var(--dev-tools-text-color);
        font-weight: 600;
        padding: 0.25rem 0.375rem;
        border-radius: 0.25rem;
      }

      .section .header .open-css:hover {
        color: var(--dev-tools-text-color-emphasis);
      }
    `;
  }

  @property({})
  public metadata!: ComponentMetadata;
  @property({})
  public theme!: ComponentTheme;

  render() {
    const sections = this.metadata.elements.map((element) => this.renderSection(element));

    return html` <div>${sections}</div> `;
  }

  private renderSection(element: ComponentElementMetadata) {
    const propertiesList = element.properties.map((property) => this.renderPropertyEditor(element, property));

    return html`
      <div class="section" data-testid=${element?.displayName}>
        <div class="header">
          <span> ${element.displayName} </span>
          <button class="open-css" @click=${() => this.handleOpenCss(element)}>Edit CSS</button>
        </div>
        <div class="property-list">${propertiesList}</div>
      </div>
    `;
  }

  private handleOpenCss(element: ComponentElementMetadata) {
    this.dispatchEvent(new OpenCssEvent(element));
  }

  private renderPropertyEditor(element: ComponentElementMetadata, property: CssPropertyMetadata) {
    let editorTagName: StaticValue;
    switch (property.editorType) {
      case EditorType.checkbox:
        editorTagName = literal`vaadin-dev-tools-theme-checkbox-property-editor`;
        break;
      case EditorType.range:
        editorTagName = literal`vaadin-dev-tools-theme-range-property-editor`;
        break;
      case EditorType.color:
        editorTagName = literal`vaadin-dev-tools-theme-color-property-editor`;
        break;
      default:
        editorTagName = literal`vaadin-dev-tools-theme-text-property-editor`;
    }

    return staticHtml` <${editorTagName}
          class="property-editor"
          .elementMetadata=${element}
          .propertyMetadata=${property}
          .theme=${this.theme}
          data-testid=${property.propertyName}
        >
        </${editorTagName}>`;
  }
}
