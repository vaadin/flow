import { css, html, LitElement, TemplateResult } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { PickerProvider } from '../component-picker';
import { metadataRegistry } from './metadata/registry';
import { icons } from './icons';
import {
  ComponentTheme,
  createScopedSelector,
  generateThemeRule,
  SelectorScope,
  ThemeContext,
  ThemeEditorState,
  ThemeScope
} from './model';
import { detectElementDisplayName, detectTheme } from './detector';
import { ThemePropertyValueChangeEvent } from './components/editors/base-property-editor';
import { themePreview } from './preview';
import { Connection } from '../connection';
import { ThemeEditorApi } from './api';
import { ThemeEditorHistory, ThemeEditorHistoryActions } from './history';
import { ScopeChangeEvent } from './components/scope-selector';
import './components/scope-selector';
import './components/property-list';
import '../component-picker.js';
import { ComponentReference } from '../component-util';
import { injectGlobalCss } from './styles';

injectGlobalCss(css`
  .vaadin-theme-editor-highlight {
    outline: solid 2px #9e2cc6;
    outline-offset: 3px;
  }
`);

@customElement('vaadin-dev-tools-theme-editor')
export class ThemeEditor extends LitElement {
  @property({})
  public themeEditorState: ThemeEditorState = ThemeEditorState.enabled;
  @property({})
  public pickerProvider!: PickerProvider;
  @property({})
  public connection!: Connection;
  private api!: ThemeEditorApi;
  private history!: ThemeEditorHistory;
  @state()
  private historyActions?: ThemeEditorHistoryActions;

  @state()
  private context: ThemeContext | null = null;

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
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
        padding: var(--theme-editor-section-horizontal-padding);
      }

      .picker {
        flex: 0 0 auto;
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .picker .instance-name {
        color: #e5a2fce5;
      }

      .picker .no-selection {
        font-style: italic;
      }

      .actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .property-list {
        flex: 1 1 auto;
        overflow-y: auto;
      }

      .link-button {
        all: initial;
        font-family: inherit;
        font-size: var(--dev-tools-font-size-small);
        line-height: 1;
        white-space: nowrap;
        color: inherit;
        font-weight: 600;
        text-decoration: underline;
      }

      .link-button:focus,
      .link-button:hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      .icon-button {
        padding: 0;
        line-height: 0;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
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

  disconnectedCallback() {
    super.disconnectedCallback();

    this.removeElementHighlight(this.context?.component.element);
  }

  render() {
    if (this.themeEditorState === ThemeEditorState.missing_theme) {
      return this.renderMissingThemeNotice();
    }

    return html`
      <div class="header">
        ${this.renderPicker()}
        <div class="actions">
          ${this.context
            ? html` <vaadin-dev-tools-theme-scope-selector
                .value=${this.context.scope}
                .metadata=${this.context.metadata}
                @scope-change=${this.handleScopeChange}
              ></vaadin-dev-tools-theme-scope-selector>`
            : null}
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
      ${this.renderPropertyList()}
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

  renderPropertyList() {
    if (!this.context) {
      return null;
    }

    const inaccessible = this.context.scope === ThemeScope.local && !this.context.accessible;
    if (inaccessible) {
      const componentName = this.context.metadata.displayName;
      return html`
        <div class="notice">
          The selected ${componentName} can not be styled locally. Currently, theme editor only supports styling
          instances that are assigned to a local variable, like so:
          <pre><code>Button saveButton = new Button("Save");</code></pre>
          If you want to modify the code so that it satisfies this requirement,
          <button class="link-button" @click=${this.handleShowComponent}>click here</button>
          to open it in your IDE. Alternatively you can choose to style all ${componentName}s by selecting "Global" from
          the scope dropdown above.
        </div>
      `;
    }

    return html` <vaadin-dev-tools-theme-property-list
      class="property-list"
      .metadata=${this.context.metadata}
      .theme=${this.effectiveTheme}
      @theme-property-value-change=${this.handlePropertyChange}
    ></vaadin-dev-tools-theme-property-list>`;
  }

  handleShowComponent() {
    if (!this.context) {
      return;
    }
    const component = this.context.component;
    const serializableComponentRef: ComponentReference = { nodeId: component.nodeId, uiId: component.uiId };
    this.connection.sendShowComponentCreateLocation(serializableComponentRef);
  }

  renderPicker() {
    let label: TemplateResult;

    if (this.context) {
      const componentDisplayName =
        this.context.scope === ThemeScope.local
          ? this.context.metadata.displayName
          : `All ${this.context.metadata.displayName}s`;
      const componentLabel = html`<span>${componentDisplayName}</span>`;
      const instanceName =
        this.context.scope === ThemeScope.local ? detectElementDisplayName(this.context.component) : null;
      const instanceLabel = instanceName ? html` <span class="instance-name">"${instanceName}"</span>` : null;
      label = html`${componentLabel} ${instanceLabel}`;
    } else {
      label = html`<span class="no-selection">Pick an element to get started</span>`;
    }
    return html`
      <div class="picker">
        <button class="icon-button" @click=${this.pickComponent}>${icons.crosshair}</button>
        ${label}
      </div>
    `;
  }

  private async pickComponent() {
    this.removeElementHighlight(this.context?.component.element);

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
          this.context = null;
          this.baseTheme = null;
          this.editedTheme = null;
          this.effectiveTheme = null;
          return;
        }

        this.highlightElement(component.element);
        const componentResponse = await this.api.loadComponentMetadata(component);
        this.previewGeneratedClassName(component.element, componentResponse.className);

        this.refreshTheme({
          scope: this.context?.scope || ThemeScope.local,
          metadata,
          component,
          localClassName: componentResponse.className,
          suggestedClassName: componentResponse.suggestedClassName,
          accessible: componentResponse.accessible
        });
      }
    });
  }

  private handleScopeChange(e: ScopeChangeEvent) {
    if (!this.context) {
      return;
    }

    this.refreshTheme({
      ...this.context,
      scope: e.detail.value
    });
  }

  private async handlePropertyChange(e: ThemePropertyValueChangeEvent) {
    if (!this.context || !this.baseTheme || !this.editedTheme) {
      return;
    }

    // Update theme state
    const { element, property, value } = e.detail;
    this.editedTheme.updatePropertyValue(element.selector, property.propertyName, value, true);
    this.effectiveTheme = ComponentTheme.combine(this.baseTheme, this.editedTheme);

    // If we are theming locally, and the component does not have a local class
    // name yet, then apply suggested class name from server first
    if (this.context.scope === ThemeScope.local && !this.context.localClassName && this.context.suggestedClassName) {
      const newClassName = this.context.suggestedClassName;
      this.context.localClassName = newClassName;
      await this.api.setComponentClassName(this.context.component, newClassName);
      this.previewGeneratedClassName(this.context.component.element, newClassName);
    }

    // Can't generate a local scoped selector without a local classname
    if (this.context.scope === ThemeScope.global && !this.context.localClassName) {
      console.error('Failed to update property value because selected component does not have a local class name');
      return;
    }

    // Update theme editor CSS
    const selectorScope: SelectorScope = {
      themeScope: this.context.scope,
      localClassName: this.context.localClassName
    };
    const updateRule = generateThemeRule(element, selectorScope, property.propertyName, value);
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

  private async refreshTheme(newContext?: ThemeContext) {
    const context = newContext || this.context;
    if (!context) {
      return;
    }

    // Always update preview, this can always change even if there is no
    // selected component, or it's inaccessible. For example after an undo or
    // redo. Also preview styles need to be updated before detecting the base
    // theme / default property values for property editors.
    await this.updateThemePreview();

    // Skip refreshing the theme state if the component is not accessible
    const inaccessible = context.scope === ThemeScope.local && !context.accessible;
    if (inaccessible) {
      this.context = context;
      this.baseTheme = null;
      this.editedTheme = null;
      this.effectiveTheme = null;
    }

    // Load rules for current scope
    // Can be skipped when using local scope and element does not have a local
    // class name yet
    let editedTheme: ComponentTheme = new ComponentTheme(context.metadata);

    const hasNoPreviousRules = context.scope === ThemeScope.local && !context.localClassName;
    if (!hasNoPreviousRules) {
      const selectorScope: SelectorScope = {
        themeScope: context.scope,
        localClassName: context.localClassName
      };
      const scopedSelectors = context.metadata.elements.map((element) => createScopedSelector(element, selectorScope));
      const rulesResponse = await this.api.loadRules(scopedSelectors);
      editedTheme = ComponentTheme.fromServerRules(context.metadata, selectorScope, rulesResponse.rules);
    }

    // Update theme state after data has loaded, so that everything consistently
    // updates at once - this avoids re-rendering the editor with new metadata
    // but without matching theme state
    this.context = context;
    this.baseTheme = detectTheme(context.metadata);
    this.editedTheme = editedTheme;
    this.effectiveTheme = ComponentTheme.combine(this.baseTheme!, this.editedTheme);
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

  private highlightElement(element?: HTMLElement) {
    if (element) {
      element.classList.add('vaadin-theme-editor-highlight');
    }
  }

  private removeElementHighlight(element?: HTMLElement) {
    if (element) {
      element.classList.remove('vaadin-theme-editor-highlight');
    }
  }

  /**
   * Adds instance class name generated by the server to the specified element,
   * so that instance-specific styles can be previewed.
   * @param element
   * @param className
   * @private
   */
  private previewGeneratedClassName(element?: HTMLElement, className?: string) {
    if (!className || !element) {
      return;
    }

    element.classList.add(className);
  }
}
