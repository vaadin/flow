import { html, TemplateResult } from 'lit';
import { customElement } from 'lit/decorators.js';
import { BasePropertyEditor } from './base-property-editor';

@customElement('vaadin-dev-tools-theme-text-property-editor')
export class TextPropertyEditor extends BasePropertyEditor {
  handleInputChange(e: Event) {
    const input = e.target as HTMLInputElement;
    this.dispatchChange(input.value);
  }

  protected renderEditor(): TemplateResult {
    return html` <input class="input" .value=${this.value} @change=${this.handleInputChange} /> `;
  }
}
