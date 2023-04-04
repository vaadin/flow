import { css, html, LitElement, PropertyValues } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { editorRowStyles } from '../styles';
import { TextInputChangeEvent } from './editors/base-property-editor';
import './editors/base-property-editor';

export class ClassNameChangeEvent extends CustomEvent<{ value: string }> {
  constructor(value: string) {
    super('class-name-change', { detail: { value } });
  }
}

@customElement('vaadin-dev-tools-theme-class-name-editor')
export class ClassNameEditor extends LitElement {
  static get styles() {
    return [
      editorRowStyles,
      css`
        .editor-row {
          padding-top: 0;
        }

        .editor-row .editor .error {
          display: inline-block;
          color: var(--dev-tools-red-color);
          margin-top: 4px;
        }
      `
    ];
  }

  @property({})
  public className!: string;

  @state()
  private editedClassName: string = '';
  @state()
  private invalid: boolean = false;

  protected update(changedProperties: PropertyValues) {
    super.update(changedProperties);

    if (changedProperties.has('className')) {
      this.editedClassName = this.className;
      this.invalid = false;
    }
  }

  render() {
    return html` <div class="editor-row local-class-name">
      <div class="label">CSS class name</div>
      <div class="editor">
        <vaadin-dev-tools-theme-text-input
          type="text"
          .value=${this.editedClassName}
          @change=${this.handleInputChange}
        ></vaadin-dev-tools-theme-text-input>
        ${this.invalid ? html`<br /><span class="error">Please enter a valid CSS class name</span>` : null}
      </div>
    </div>`;
  }

  private handleInputChange(e: TextInputChangeEvent) {
    this.editedClassName = e.detail.value;

    const classNameRegex = /^-?[_a-zA-Z]+[_a-zA-Z0-9-]*$/;
    this.invalid = !this.editedClassName.match(classNameRegex);

    if (!this.invalid && this.editedClassName !== this.className) {
      this.dispatchEvent(new ClassNameChangeEvent(this.editedClassName));
    }
  }
}
