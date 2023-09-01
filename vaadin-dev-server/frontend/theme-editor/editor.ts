import { css, html, LitElement, PropertyValues, TemplateResult } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { PickerProvider } from '../component-picker';
import { metadataRegistry } from './metadata/registry';
import { icons } from './icons';
import {
  ComponentTheme,
  createScopedSelector,
  generateThemeRule,
  generateThemeRuleCss,
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
import './components/class-name-editor';
import './components/scope-selector';
import './components/property-list';
import '../component-picker.js';
import { ComponentReference } from '../component-util';
import { injectGlobalCss } from './styles';
import { ComponentMetadata } from './metadata/model';
import { ClassNameChangeEvent } from './components/class-name-editor';
import { OpenCssEvent } from './components/property-list';
import { componentOverlayManager } from './components/component-overlay-manager';

injectGlobalCss(css`
  .vaadin-theme-editor-highlight {
    outline: solid 2px #9e2cc6;
    outline-offset: 3px;
  }
`);

@customElement('vaadin-dev-tools-theme-editor')
export class ThemeEditor extends LitElement {
  @property({})
  public expanded: boolean = false;
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

  private undoRedoListener;

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

      .hint vaadin-icon {
        color: var(--dev-tools-green-color);
        font-size: var(--lumo-icon-size-m);
      }

      .hint {
        display: flex;
        align-items: center;
        gap: var(--theme-editor-section-horizontal-padding);
      }

      .header {
        flex: 0 0 auto;
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }

      .header .picker-row {
        padding: var(--theme-editor-section-horizontal-padding);
        display: flex;
        gap: 20px;
        align-items: center;
        justify-content: space-between;
      }

      .picker {
        flex: 1 1 0;
        min-width: 0;
        display: flex;
        align-items: center;
      }

      .picker button {
        min-width: 0;
        display: inline-flex;
        align-items: center;
        padding: 0;
        line-height: 20px;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
      }

      .picker button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      .picker svg,
      .picker .component-type {
        flex: 0 0 auto;
        margin-right: 4px;
      }

      .picker .instance-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #e5a2fce5;
      }

      .picker .instance-name-quote {
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
    this.api.markAsUsed();

    this.undoRedoListener = (evt: KeyboardEvent) => {
      const isZKey = evt.key === 'Z' || evt.key === 'z';
      if (isZKey && (evt.ctrlKey || evt.metaKey) && evt.shiftKey) {
        if (this.historyActions?.allowRedo) {
          this.handleRedo();
        }
      } else if (isZKey && (evt.ctrlKey || evt.metaKey)) {
        if (this.historyActions?.allowUndo) {
          this.handleUndo();
        }
      }
    }

    // When the theme is updated due to HMR, remove optimistic updates from
    // theme preview. Also refresh the base theme as default property values may
    // have changed.
    document.addEventListener('vaadin-theme-updated', () => {
      themePreview.clear();
      this.refreshTheme();
    });

    document.addEventListener('keydown', this.undoRedoListener);

    this.dispatchEvent(new CustomEvent('before-open'));
  }

  protected update(changedProperties: PropertyValues) {
    super.update(changedProperties);

    // Remove or restore selected element highlight when expanded state changes
    if (changedProperties.has('expanded')) {
      if (this.expanded) {
        this.highlightElement(this.context?.component.element);
        componentOverlayManager.showOverlay();
      } else {
        componentOverlayManager.hideOverlay();
        this.removeElementHighlight(this.context?.component.element);
      }
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.removeElementHighlight(this.context?.component.element);

    componentOverlayManager.hideOverlay();
    componentOverlayManager.reset();

    document.removeEventListener('keydown', this.undoRedoListener);

    this.dispatchEvent(new CustomEvent('after-close'));
  }

  render() {
    if (this.themeEditorState === ThemeEditorState.missing_theme) {
      return this.renderMissingThemeNotice();
    }

    return html`
      <div class="header">
        <div class="picker-row">
          ${this.renderPicker()}
          <div class="actions">
            ${this.context?.metadata
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
        ${this.renderLocalClassNameEditor()}
      </div>
      ${this.renderPropertyList()}
    `;
  }

  renderMissingThemeNotice() {
    return html`
      <div class="notice">
        It looks like you have not set up an application theme yet. Theme editor requires an existing theme to work
        with. Please check our
        <a href="https://vaadin.com/docs/latest/styling/application-theme" target="_blank">documentation</a>
        on how to set up an application theme.
      </div>
    `;
  }

  renderPropertyList() {
    if (!this.context) {
      return null;
    }

    // If there is no metadata, then we have a component that is not supported
    if (!this.context.metadata) {
      const tagName = this.context.component.element!.localName;
      return html`
        <div class="notice">Styling <code>&lt;${tagName}&gt;</code> components is not supported at the moment.</div>
      `;
    }

    const inaccessible = this.context.scope === ThemeScope.local && !this.context.accessible;
    if (inaccessible) {
      const componentName = this.context.metadata.displayName;
      return html`
        ${this.context.metadata.notAccessibleDescription && this.context.scope === ThemeScope.local
          ? html`<div class="notice hint" style="padding-bottom: 0;">
              <vaadin-icon icon="vaadin:lightbulb"></vaadin-icon>
              <div>${this.context.metadata.notAccessibleDescription}</div>
            </div>`
          : ''}
        <div class="notice">
          The selected ${componentName} cannot be styled locally. Currently, Theme Editor only supports styling
          instances that are assigned to a local variable, like so:
          <pre><code>Button saveButton = new Button("Save");</code></pre>
          If you want to modify the code so that it satisfies this requirement,
          <button class="link-button" @click=${this.handleShowComponent}>click here</button>
          to open it in your IDE. Alternatively you can choose to style all ${componentName}s by selecting "Global" from
          the scope dropdown above.
        </div>
      `;
    }

    return html` ${this.context.metadata.description && this.context.scope === ThemeScope.local
        ? html`<div class="notice hint">
            <vaadin-icon icon="vaadin:lightbulb"></vaadin-icon>
            <div>${this.context.metadata.description}</div>
          </div>`
        : ''}
      <vaadin-dev-tools-theme-property-list
        class="property-list"
        .metadata=${this.context.metadata}
        .theme=${this.effectiveTheme}
        @theme-property-value-change=${this.handlePropertyChange}
        @open-css=${this.handleOpenCss}
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

  async handleOpenCss(event: OpenCssEvent) {
    if (!this.context) {
      return;
    }

    // If we are theming locally, and the component does not have a local class
    // name yet, then apply suggested class name from server first
    await this.ensureLocalClassName();

    const selectorScope: SelectorScope = {
      themeScope: this.context.scope,
      localClassName: this.context.localClassName
    };
    const scopedSelector = createScopedSelector(event.detail.element, selectorScope);
    await this.api.openCss(scopedSelector);
  }

  renderPicker() {
    let label: TemplateResult;

    if (this.context?.metadata) {
      const componentDisplayName =
        this.context.scope === ThemeScope.local
          ? this.context.metadata.displayName
          : `All ${this.context.metadata.displayName}s`;
      const componentLabel = html`<span class="component-type">${componentDisplayName}</span>`;
      const instanceName =
        this.context.scope === ThemeScope.local ? detectElementDisplayName(this.context.component) : null;
      const instanceLabel = instanceName
        ? html` <span class="instance-name-quote">"</span><span class="instance-name">${instanceName}</span
            ><span class="instance-name-quote">"</span>`
        : null;
      label = html`${componentLabel} ${instanceLabel}`;
    } else {
      label = html`<span class="no-selection">Pick an element to get started</span>`;
    }
    return html`
      <div class="picker">
        <button @click=${this.pickComponent}>${icons.crosshair} ${label}</button>
      </div>
    `;
  }

  renderLocalClassNameEditor() {
    const allowEditing = this.context?.scope === ThemeScope.local && this.context.accessible;
    if (!this.context || !allowEditing) {
      return null;
    }

    const instanceClassName = this.context.localClassName || this.context.suggestedClassName;

    return html` <vaadin-dev-tools-theme-class-name-editor
      .className=${instanceClassName}
      @class-name-change=${this.handleClassNameChange}
    >
    </vaadin-dev-tools-theme-class-name-editor>`;
  }

  private async handleClassNameChange(e: ClassNameChangeEvent) {
    if (!this.context) {
      return;
    }

    const previousClassName = this.context.localClassName;
    const newClassName = e.detail.value;
    if (previousClassName) {
      // Update local class name if there is an existing one
      const element = this.context.component.element;
      this.context.localClassName = newClassName;
      const classNameResponse = await this.api.setLocalClassName(this.context.component, newClassName);
      this.historyActions = this.history.push(
        classNameResponse.requestId,
        () => themePreview.previewLocalClassName(element, newClassName),
        () => themePreview.previewLocalClassName(element, previousClassName)
      );
    } else {
      // Update suggested class name for now, will effectively be applied when
      // changing a property
      this.context = {
        ...this.context,
        suggestedClassName: newClassName
      };
    }
  }

  private async pickComponent() {
    componentOverlayManager.hideOverlay();
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
          this.context = { component, scope: this.context?.scope || ThemeScope.local };
          this.baseTheme = null;
          this.editedTheme = null;
          this.effectiveTheme = null;
          return;
        }
        await componentOverlayManager.componentPicked(component, metadata);
        this.highlightElement(component.element);
        this.refreshComponentAndTheme(component, metadata);
        componentOverlayManager.showOverlay();
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
    await this.ensureLocalClassName();

    // Update theme editor CSS
    const selectorScope: SelectorScope = {
      themeScope: this.context.scope,
      localClassName: this.context.localClassName
    };
    const updateRule = generateThemeRule(element, selectorScope, property.propertyName, value);
    try {
      const response = await this.api.setCssRules([updateRule]);
      this.historyActions = this.history.push(response.requestId);
      // Do optimistic update of property changes, will be cleared after HMR of
      // theme files
      const css = generateThemeRuleCss(updateRule);
      themePreview.add(css);
    } catch (error) {
      console.error('Failed to update property value', error);
    }
  }

  private async handleUndo() {
    this.historyActions = await this.history.undo();
    await this.refreshComponentAndTheme();
  }

  private async handleRedo() {
    this.historyActions = await this.history.redo();
    await this.refreshComponentAndTheme();
  }

  private async ensureLocalClassName() {
    // Don't need to do anything if we are theming globally, or a class name is
    // already defined
    if (!this.context || this.context.scope === ThemeScope.global || this.context.localClassName) {
      return;
    }

    // Fail if there is neither a class name, nor a suggested class name
    if (!this.context.localClassName && !this.context.suggestedClassName) {
      throw new Error(
        'Cannot assign local class name for the component because it does not have a suggested class name'
      );
    }

    const element = this.context.component.element;
    const newClassName = this.context.suggestedClassName!;
    this.context.localClassName = newClassName;
    const classNameResponse = await this.api.setLocalClassName(this.context.component, newClassName);
    this.historyActions = this.history.push(
      classNameResponse.requestId,
      () => themePreview.previewLocalClassName(element, newClassName),
      () => themePreview.previewLocalClassName(element)
    );
  }

  private async refreshComponentAndTheme(component?: ComponentReference, metadata?: ComponentMetadata) {
    component = component || this.context?.component;
    metadata = metadata || this.context?.metadata;
    if (!component || !metadata) {
      return;
    }

    const componentResponse = await this.api.loadComponentMetadata(component);
    themePreview.previewLocalClassName(component.element, componentResponse.className);

    await this.refreshTheme({
      scope: this.context?.scope || ThemeScope.local,
      metadata,
      component,
      localClassName: componentResponse.className,
      suggestedClassName: componentResponse.suggestedClassName,
      accessible: componentResponse.accessible
    });
  }

  private async refreshTheme(newContext?: ThemeContext) {
    const context = newContext || this.context;
    if (!context || !context.metadata) {
      return;
    }

    // Skip refreshing the theme state if the component is not accessible
    const inaccessible = context.scope === ThemeScope.local && !context.accessible;
    if (inaccessible) {
      this.context = context;
      this.baseTheme = null;
      this.editedTheme = null;
      this.effectiveTheme = null;
      return;
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
    const baseTheme = await detectTheme(context.metadata);
    this.context = context;
    this.baseTheme = baseTheme;
    this.editedTheme = editedTheme;
    this.effectiveTheme = ComponentTheme.combine(baseTheme, this.editedTheme);
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
}
