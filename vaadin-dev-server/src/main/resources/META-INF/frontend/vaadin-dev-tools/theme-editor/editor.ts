import { css, html, LitElement } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { PickerProvider } from '../component-picker';
import { ComponentMetadata } from './metadata/model';
import { metadataRegistry } from './metadata/registry';
import { icons } from './icons';
import './property-list';
import { ThemeEditorState } from './model';

@customElement('vaadin-dev-tools-theme-editor')
export class ThemeEditor extends LitElement {
  @property({})
  public themeEditorState: ThemeEditorState = ThemeEditorState.enabled;
  @property({})
  public pickerProvider!: PickerProvider;

  @state()
  private selectedComponentMetadata: ComponentMetadata | null = null;

  static get styles() {
    return css`
      :host {
        animation: fade-in var(--dev-tools-transition-duration) ease-in;
        --theme-editor-section-horizontal-padding: 0.75rem;
      }

      .missing-theme {
        padding: var(--theme-editor-section-horizontal-padding);
      }

      .missing-theme a {
        color: var(--dev-tools-text-color-emphasis);
      }

      .picker {
        display: flex;
        align-items: center;
        padding: var(--theme-editor-section-horizontal-padding);
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }

      .picker > button {
        padding: 0;
        line-height: 0;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
        margin-right: 0.5rem;
      }

      .picker > button:hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      .picker .no-selection {
        font-style: italic;
      }
    `;
  }

  render() {
    if (this.themeEditorState === ThemeEditorState.missing_theme) {
      return this.renderMissingThemeNotice();
    }

    return html`
      <div class="picker">
        <button class="button" @click=${this.pickComponent}>${icons.crosshair}</button>
        ${this.selectedComponentMetadata
          ? html`<span>${this.selectedComponentMetadata.displayName}</span>`
          : html`<span class="no-selection">Pick an element to get started</span>`}
      </div>
      ${this.selectedComponentMetadata
        ? html` <vaadin-dev-tools-theme-property-list
            .metadata=${this.selectedComponentMetadata}
          ></vaadin-dev-tools-theme-property-list>`
        : null}
    `;
  }

  renderMissingThemeNotice() {
    return html`
      <div class="missing-theme">
        It looks like you have not set up a custom theme yet. Theme editor requires an existing theme to work with.
        Please check our
        <a href="https://vaadin.com/docs/latest/styling/custom-theme/creating-custom-theme" target="_blank"
          >documentation</a
        >
        on how to set up a custom theme.
      </div>
    `;
  }

  private async pickComponent() {
    // Ensure component picker module is loaded
    await import('../component-picker.js');

    this.pickerProvider().open({
      infoTemplate: html`
        <div>
          <h3>Locate the component to style</h3>
          <p>Use the mouse cursor to highlight components in the UI.</p>
          <p>Use arrow down/up to cycle through and highlight specific components under the cursor.</p>
          <p>Click the primary mouse button to select the component.</p>
        </div>
      `,
      pickCallback: async (component) => {
        this.selectedComponentMetadata = await metadataRegistry.getMetadata(component);
      }
    });
  }
}
