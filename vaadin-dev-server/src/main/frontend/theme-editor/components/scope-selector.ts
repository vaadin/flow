import { css, html, LitElement, PropertyValues, render } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';
import { Select, SelectValueChangedEvent } from '@vaadin/select';
import { ComponentMetadata } from '../metadata/model';
import { ThemeScope } from '../model';
import { injectGlobalCss } from '../styles';

export class ScopeChangeEvent extends CustomEvent<{ value: ThemeScope }> {
  constructor(value: ThemeScope) {
    super('scope-change', { detail: { value } });
  }
}

injectGlobalCss(css`
  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] {
    --lumo-primary-color-50pct: rgba(255, 255, 255, 0.5);
    z-index: 100000 !important;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector']::part(overlay) {
    background: #333;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item {
    color: rgba(255, 255, 255, 0.8);
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(content) {
    font-size: 13px;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item .title {
    color: rgba(255, 255, 255, 0.95);
    font-weight: bold;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(checkmark) {
    margin: 6px;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(checkmark)::before {
    color: rgba(255, 255, 255, 0.95);
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item:hover {
    background: rgba(255, 255, 255, 0.1);
  }
`);

@customElement('vaadin-dev-tools-theme-scope-selector')
export class ScopeSelector extends LitElement {
  static get styles() {
    return css`
      vaadin-select {
        --lumo-primary-color-50pct: rgba(255, 255, 255, 0.5);
        width: 100px;
      }

      vaadin-select::part(input-field) {
        background: rgba(0, 0, 0, 0.2);
      }

      vaadin-select vaadin-select-value-button,
      vaadin-select::part(toggle-button) {
        color: var(--dev-tools-text-color);
      }

      vaadin-select:hover vaadin-select-value-button,
      vaadin-select:hover::part(toggle-button) {
        color: var(--dev-tools-text-color-emphasis);
      }

      vaadin-select vaadin-select-item {
        font-size: 13px;
      }
    `;
  }

  @property({})
  public value: ThemeScope = ThemeScope.local;
  @property({})
  public metadata?: ComponentMetadata;
  @query('vaadin-select')
  private select?: Select;

  protected update(changedProperties: PropertyValues) {
    super.update(changedProperties);

    if (changedProperties.has('metadata')) {
      this.select?.requestContentUpdate();
    }
  }

  render() {
    return html` <vaadin-select
      theme="small vaadin-dev-tools-theme-scope-selector"
      .value=${this.value}
      .renderer=${this.selectRenderer.bind(this)}
      @value-changed=${this.handleValueChange}
    ></vaadin-select>`;
  }

  private selectRenderer(root: HTMLElement) {
    const componentName = this.metadata?.displayName || 'Component';
    const componentNamePlural = `${componentName}s`;

    render(
      html`
        <vaadin-list-box>
          <vaadin-item value=${ThemeScope.local} label="Local">
            <span class="title">Local</span>
            <br />
            <span>Edit styles for this ${componentName}</span>
          </vaadin-item>
          <vaadin-item value=${ThemeScope.global} label="Global">
            <span class="title">Global</span>
            <br />
            <span>Edit styles for all ${componentNamePlural}</span>
          </vaadin-item>
        </vaadin-list-box>
      `,
      root
    );
  }

  private handleValueChange(e: SelectValueChangedEvent) {
    // Discard change if it was caused from setting value property
    const newScopeType = e.detail.value as ThemeScope;
    if (newScopeType === this.value) {
      return;
    }
    this.dispatchEvent(new ScopeChangeEvent(newScopeType));
  }
}
