import { css, html, LitElement } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { PickerProvider } from '../component-picker';
import { ComponentMetadata } from './metadata/model';
import { metadataRegistry } from './metadata/registry';
import { icons } from './icons';
import './property-list';
import { ComponentTheme, generateRules, Theme, ThemeEditorState } from './model';
import { detectTheme } from './detector';
import { ThemePropertyValueChangeEvent } from './events';
import { themePreview } from './preview';
import { Connection } from '../vaadin-dev-tools';

@customElement('vaadin-dev-tools-theme-editor')
export class ThemeEditor extends LitElement {
  @property({})
  public themeEditorState: ThemeEditorState = ThemeEditorState.enabled;
  @property({})
  public pickerProvider!: PickerProvider;
  @property({})
  public connection!: Connection;

  /**
   * Metadata for the selected / picked component
   */
  @state()
  private selectedComponentMetadata: ComponentMetadata | null = null;
  /**
   * Theme modifications for all components, containing all changes since the
   * last reload
   */
  private theme: Theme = new Theme();
  /**
   * Base theme detected from existing CSS files for the selected component
   */
  private baseComponentTheme: ComponentTheme | null = null;
  /**
   * Previously saved theme modifications for the selected component since the
   * last reload
   */
  private modifiedComponentTheme: ComponentTheme | null = null;
  /**
   * Pending theme modifications for the selected component that have not been
   * saved yet
   */
  private editedComponentTheme: ComponentTheme | null = null;
  /**
   * The effective theme for the selected component, including base theme,
   * previously saved modifications and pending modifications
   */
  @state()
  private effectiveComponentTheme: ComponentTheme | null = null;
  @state()
  private hasModifications: boolean = false;

  static get styles() {
    return css`
      :host {
        animation: fade-in var(--dev-tools-transition-duration) ease-in;
        --theme-editor-section-horizontal-padding: 0.75rem;
        display: flex;
        flex-direction: column;
        max-height: 400px;
      }

      .missing-theme {
        padding: var(--theme-editor-section-horizontal-padding);
      }

      .missing-theme a {
        color: var(--dev-tools-text-color-emphasis);
      }

      .picker {
        flex: 0 0 auto;
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

      .property-list {
        flex: 1 1 auto;
        overflow-y: auto;
      }

      .modifications-actions {
        flex: 0 0 auto;
        display: flex;
        justify-content: space-between;
        padding: 0.5rem var(--theme-editor-section-horizontal-padding);
        border-top: solid 1px rgba(0, 0, 0, 0.2);
      }

      .modifications-actions button {
        all: initial;
        font-family: inherit;
        font-size: var(--dev-tools-font-size-small);
        line-height: 1;
        white-space: nowrap;
        background-color: rgba(0, 0, 0, 0.2);
        color: inherit;
        font-weight: 600;
        padding: 0.25rem 0.375rem;
        border-radius: 0.25rem;
      }

      .modifications-actions button.discard {
        color: var(--dev-tools-text-color-emphasis);
        background-color: var(--dev-tools-red-color);
      }

      .modifications-actions button.apply {
        color: var(--dev-tools-text-color-emphasis);
        background-color: var(--dev-tools-green-color);
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
            class="property-list"
            .metadata=${this.selectedComponentMetadata}
            .theme=${this.effectiveComponentTheme}
            @theme-property-value-change=${this.handlePropertyChange}
          ></vaadin-dev-tools-theme-property-list>`
        : null}
      ${this.hasModifications
        ? html`
            <div class="modifications-actions">
              <button class="discard" @click=${this.discardChanges}>Discard</button>
              <button class="apply" @click=${this.applyChanges}>Apply changes</button>
            </div>
          `
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
        this.hasModifications = false;
        if (this.selectedComponentMetadata) {
          themePreview.reset();
          this.baseComponentTheme = detectTheme(this.selectedComponentMetadata);
          this.modifiedComponentTheme =
            this.theme.getComponentTheme(this.selectedComponentMetadata.tagName) ||
            new ComponentTheme(this.selectedComponentMetadata);
          this.editedComponentTheme = new ComponentTheme(this.selectedComponentMetadata);
          this.effectiveComponentTheme = ComponentTheme.combine(this.baseComponentTheme, this.modifiedComponentTheme);
        } else {
          this.baseComponentTheme = null;
          this.modifiedComponentTheme = null;
          this.editedComponentTheme = null;
          this.effectiveComponentTheme = null;
        }
        this.updateThemePreview();
      }
    });
  }

  private handlePropertyChange(e: ThemePropertyValueChangeEvent) {
    if (!this.editedComponentTheme || !this.baseComponentTheme || !this.modifiedComponentTheme) {
      return;
    }
    const { part, property, value } = e.detail;
    this.hasModifications = true;
    this.editedComponentTheme.updatePropertyValue(part.partName, property.propertyName, value);
    this.effectiveComponentTheme = ComponentTheme.combine(
      this.baseComponentTheme,
      this.modifiedComponentTheme,
      this.editedComponentTheme
    );
    this.updateThemePreview();
  }

  private discardChanges() {
    if (!this.selectedComponentMetadata || !this.baseComponentTheme || !this.modifiedComponentTheme) {
      return;
    }
    this.hasModifications = false;
    this.editedComponentTheme = new ComponentTheme(this.selectedComponentMetadata);
    this.effectiveComponentTheme = ComponentTheme.combine(this.baseComponentTheme, this.modifiedComponentTheme);
    this.updateThemePreview();
  }

  private applyChanges() {
    if (!this.editedComponentTheme || !this.selectedComponentMetadata || !this.baseComponentTheme) {
      return;
    }

    // Notify dev tools that we are about to save CSS, so that it can disable
    // live reload temporarily
    this.dispatchEvent(new CustomEvent('before-save'));

    const rules = generateRules(this.editedComponentTheme);
    this.connection.sendThemeEditorRules(rules);

    this.hasModifications = false;
    this.theme.updateComponentTheme(this.editedComponentTheme);
    this.modifiedComponentTheme = this.theme.getComponentTheme(this.selectedComponentMetadata.tagName);
    this.editedComponentTheme = new ComponentTheme(this.selectedComponentMetadata);
    this.effectiveComponentTheme = ComponentTheme.combine(this.baseComponentTheme, this.modifiedComponentTheme);
  }

  private updateThemePreview() {
    const previewTheme = this.theme.clone();
    if (this.editedComponentTheme) {
      previewTheme.updateComponentTheme(this.editedComponentTheme);
    }
    themePreview.update(previewTheme);
  }
}
