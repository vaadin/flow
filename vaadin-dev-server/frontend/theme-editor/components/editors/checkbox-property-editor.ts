import { html, css, TemplateResult } from 'lit';
import { customElement } from 'lit/decorators.js';
import { BasePropertyEditor } from './base-property-editor';

@customElement('vaadin-dev-tools-theme-checkbox-property-editor')
export class CheckboxPropertyEditor extends BasePropertyEditor {
  static get styles() {
    return [
      BasePropertyEditor.styles,
      css`
        .editor-row {
          align-items: center;
        }
      `
    ];
  }

  handleInputChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const value = input.checked ? this.propertyMetadata.checkedValue : '';
    this.dispatchChange(value || '');
  }

  protected renderEditor(): TemplateResult {
    const checked = this.value === this.propertyMetadata.checkedValue;
    return html` <input type="checkbox" .checked=${checked} @change=${this.handleInputChange} /> `;
  }
}
