import { html, TemplateResult } from 'lit';
import { customElement } from 'lit/decorators.js';
import { BasePropertyEditor, TextInputChangeEvent } from './base-property-editor';

@customElement('vaadin-dev-tools-theme-text-property-editor')
export class TextPropertyEditor extends BasePropertyEditor {
  handleInputChange(e: TextInputChangeEvent) {
    this.dispatchChange(e.detail.value);
  }

  protected renderEditor(): TemplateResult {
    return html`
      <vaadin-dev-tools-theme-text-input
        .value=${this.value}
        .showClearButton=${this.propertyValue?.modified || false}
        @change=${this.handleInputChange}
      ></vaadin-dev-tools-theme-text-input>
    `;
  }
}
