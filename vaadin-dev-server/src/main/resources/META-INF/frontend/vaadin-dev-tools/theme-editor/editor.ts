import { css, html, LitElement } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { PickerProvider } from '../component-picker';
import { ComponentMetadata } from './metadata/model';
import { metadataRegistry } from './metadata/registry';
import { icons } from './icons';
import './property-list';
import { ComponentTheme, generateThemeRule, ThemeEditorLicense, ThemeEditorSettings, ThemeEditorState } from './model';
import { detectTheme } from './detector';
import { ThemePropertyValueChangeEvent } from './events';
import { themePreview } from './preview';
import { Connection } from '../vaadin-dev-tools';
import { ThemeEditorApi } from './api';
import { ThemeEditorHistory, ThemeEditorHistoryActions } from './history';

@customElement('vaadin-dev-tools-theme-editor')
export class ThemeEditor extends LitElement {
  @property({})
  public settings!: ThemeEditorSettings;
  @property({})
  public pickerProvider!: PickerProvider;
  @property({})
  public connection!: Connection;
  private api!: ThemeEditorApi;
  private history!: ThemeEditorHistory;
  @state()
  private historyActions?: ThemeEditorHistoryActions;

  /**
   * Metadata for the selected / picked component
   */
  @state()
  private selectedComponentMetadata: ComponentMetadata | null = null;
  /**
   * Base theme detected from existing CSS files for the selected component
   */
  private baseTheme: ComponentTheme | null = null;
  /**
   * Currently edited theme modifications for the selected component since the
   * last reload
   */
  private editedTheme: ComponentTheme | null = null;
  /**
   * The effective theme for the selected component, including base theme and
   * previously saved modifications
   */
  @state()
  private effectiveTheme: ComponentTheme | null = null;

  static get styles() {
    return css`
      :host {
        animation: fade-in var(--dev-tools-transition-duration) ease-in;
        --theme-editor-section-horizontal-padding: 0.75rem;
        display: flex;
        flex-direction: column;
        max-height: 400px;
      }

      .notice {
        padding: var(--theme-editor-section-horizontal-padding);
      }

      .notice a {
        color: var(--dev-tools-text-color-emphasis);
      }

      .header {
        flex: 0 0 auto;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      .picker {
        flex: 0 0 auto;
        display: flex;
        align-items: center;
        padding: var(--theme-editor-section-horizontal-padding);
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }

      .picker .no-selection {
        font-style: italic;
      }

      .actions {
        display: flex;
        align-items: center;
      }

      .property-list {
        flex: 1 1 auto;
        overflow-y: auto;
      }

      .icon-button {
        padding: 0;
        line-height: 0;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
        margin-right: 0.5rem;
      }

      .icon-button:disabled {
        opacity: 0.5;
      }

      .icon-button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }
    `;
  }

  protected firstUpdated() {
    this.api = new ThemeEditorApi(this.connection);
    this.history = new ThemeEditorHistory(this.api);
    this.historyActions = this.history.allowedActions;
  }

  render() {
    if (this.settings.license === ThemeEditorLicense.notChecked) {
      return null;
    }
    if (this.settings.license === ThemeEditorLicense.invalid) {
      return this.renderMissingLicenseNotice();
    }
    if (this.settings.state === ThemeEditorState.missing_theme) {
      return this.renderMissingThemeNotice();
    }

    return html`
      <div class="header">
        <div class="picker">
          <button class="icon-button" @click=${this.pickComponent}>${icons.crosshair}</button>
          ${this.selectedComponentMetadata
            ? html`<span>${this.selectedComponentMetadata.displayName}</span>`
            : html`<span class="no-selection">Pick an element to get started</span>`}
        </div>
        <div class="actions">
          <button
            class="icon-button"
            data-testid="undo"
            ?disabled=${!this.historyActions?.allowUndo}
            @click=${this.handleUndo}
          >
            ${icons.undo}
          </button>
          <button
            class="icon-button"
            data-testid="redo"
            ?disabled=${!this.historyActions?.allowRedo}
            @click=${this.handleRedo}
          >
            ${icons.redo}
          </button>
        </div>
      </div>
      ${this.selectedComponentMetadata
        ? html` <vaadin-dev-tools-theme-property-list
            class="property-list"
            .metadata=${this.selectedComponentMetadata}
            .theme=${this.effectiveTheme}
            @theme-property-value-change=${this.handlePropertyChange}
          ></vaadin-dev-tools-theme-property-list>`
        : null}
    `;
  }

  renderMissingLicenseNotice() {
    return html`
      <div class="notice">
        Theme editor requires a Vaadin Pro (or higher) subscription.
        <br />
        Please
        <a href=${this.settings.licenseUrl} target="_blank">log in or sign up for an account</a>.
      </div>
    `;
  }

  renderMissingThemeNotice() {
    return html`
      <div class="notice">
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
        const metadata = await metadataRegistry.getMetadata(component);
        if (!metadata) {
          this.baseTheme = null;
          this.editedTheme = null;
          this.effectiveTheme = null;
          return;
        }

        const scopeSelector = metadata.tagName;
        const serverRules = await this.api.loadRules(scopeSelector);

        this.selectedComponentMetadata = metadata;
        this.baseTheme = detectTheme(metadata);
        this.editedTheme = ComponentTheme.fromServerRules(metadata, serverRules.rules);
        this.effectiveTheme = ComponentTheme.combine(this.baseTheme, this.editedTheme);
      }
    });
  }

  private async handlePropertyChange(e: ThemePropertyValueChangeEvent) {
    if (!this.selectedComponentMetadata || !this.baseTheme || !this.editedTheme) {
      return;
    }

    // Update local theme state
    const { part, property, value } = e.detail;
    const partName = part?.partName || null;
    this.editedTheme.updatePropertyValue(partName, property.propertyName, value, true);
    this.effectiveTheme = ComponentTheme.combine(this.baseTheme, this.editedTheme);

    // Update theme editor CSS
    const updateRule = generateThemeRule(
      this.selectedComponentMetadata?.tagName,
      partName,
      property.propertyName,
      value
    );
    try {
      this.preventLiveReload();
      const response = await this.api.setCssRules([updateRule]);
      this.historyActions = this.history.push(response.requestId);
      await this.updateThemePreview();
    } catch (error) {
      console.error('Failed to update property value', error);
    }
  }

  private async handleUndo() {
    this.preventLiveReload();
    this.historyActions = await this.history.undo();
    await this.refreshTheme();
  }

  private async handleRedo() {
    this.preventLiveReload();
    this.historyActions = await this.history.redo();
    await this.refreshTheme();
  }

  private async refreshTheme() {
    if (!this.selectedComponentMetadata || !this.baseTheme) {
      return;
    }
    const scopeSelector = this.selectedComponentMetadata.tagName;
    const serverRules = await this.api.loadRules(scopeSelector);

    this.editedTheme = ComponentTheme.fromServerRules(this.selectedComponentMetadata, serverRules.rules);
    this.effectiveTheme = ComponentTheme.combine(this.baseTheme, this.editedTheme);
    await this.updateThemePreview();
  }

  private async updateThemePreview() {
    const preview = await this.api.loadPreview();
    themePreview.update(preview.css);
  }

  private preventLiveReload() {
    // Notify dev tools that we are about to save CSS, so that it can disable
    // live reload temporarily
    this.dispatchEvent(new CustomEvent('before-save'));
  }
}
