import { css, html, LitElement, PropertyValues, TemplateResult } from 'lit';
import { customElement, query, state } from 'lit/decorators.js';
import { ComponentReference, getComponents } from './component-util.js';
import './shim.js';
import { Shim } from './shim.js';
import { popupStyles } from './vaadin-dev-tools.js';

export interface PickerOptions {
  infoTemplate: TemplateResult;
  pickCallback: (component: ComponentReference) => void;
}

export type PickerProvider = () => ComponentPicker;

/**
 * When active, shows a component picker that allows the user to select an element with a server side counterpart.
 */
@customElement('vaadin-dev-tools-component-picker')
export class ComponentPicker extends LitElement {
  @state()
  active: boolean = false;

  options?: PickerOptions;

  @state()
  components: ComponentReference[] = [];
  @state()
  selected: number = 0;

  highlighted?: HTMLElement;

  @query('vaadin-dev-tools-shim')
  shim!: Shim;

  static styles = [
    popupStyles,
    css`
      .component-picker-info {
        left: 1em;
        bottom: 1em;
      }

      .component-picker-components-info {
        right: 3em;
        bottom: 1em;
      }

      .component-picker-components-info .selected {
        font-weight: bold;
      }
    `
  ];

  connectedCallback() {
    super.connectedCallback();
    const globalStyles = new CSSStyleSheet();
    globalStyles.replaceSync(`
    .vaadin-dev-tools-highlight {
      outline: 1px solid red
    }`);

    document.adoptedStyleSheets = [...document.adoptedStyleSheets, globalStyles];
  }
  render() {
    if (!this.active) {
      this.style.display = 'none';
      return null;
    }

    this.style.display = 'block';
    return html`
      <vaadin-dev-tools-shim
        @shim-click=${this.shimClick}
        @shim-mousemove=${this.shimMove}
        @shim-keydown=${this.shimKeydown}
      ></vaadin-dev-tools-shim>
      <div class="window popup component-picker-info">${this.options?.infoTemplate}</div>
      <div class="window popup component-picker-components-info">
        <div>
          ${this.components.map(
            (component, index) =>
              html`<div class=${index === this.selected ? 'selected' : ''}>
                ${component.element!.tagName.toLowerCase()}
              </div>`
          )}
        </div>
      </div>
    `;
  }

  open(options: PickerOptions) {
    this.options = options;
    this.active = true;
    this.dispatchEvent(new CustomEvent('component-picker-opened', {}));
  }

  close() {
    this.active = false;
    this.dispatchEvent(new CustomEvent('component-picker-closed', {}));
  }

  update(changedProperties: PropertyValues): void {
    super.update(changedProperties);
    if (changedProperties.has('selected') || changedProperties.has('components')) {
      this.highlight(this.components[this.selected]?.element);
    }
    if (changedProperties.has('active')) {
      const wasActive = changedProperties.get('active');
      const isActive = this.active;

      if (!wasActive && isActive) {
        // Focus shim when created so that keyboard events go there
        requestAnimationFrame(() => this.shim.focus());
      } else if (wasActive && !isActive) {
        this.highlight(undefined);
      }
    }
  }
  shimKeydown(e: CustomEvent) {
    const keyEvent: KeyboardEvent = e.detail.originalEvent;
    if (keyEvent.key === 'Escape') {
      this.close();
      e.stopPropagation();
      e.preventDefault();
    } else if (keyEvent.key === 'ArrowUp') {
      let selected = this.selected - 1;
      if (selected < 0) {
        selected = this.components.length - 1;
      }
      this.selected = selected;
    } else if (keyEvent.key === 'ArrowDown') {
      this.selected = (this.selected + 1) % this.components.length;
    } else if (keyEvent.key === 'Enter') {
      this.pickSelectedComponent();
      e.stopPropagation();
      e.preventDefault();
    }
  }
  shimMove(e: CustomEvent) {
    const targetElement = e.detail.target;

    this.components = getComponents(targetElement);
    this.selected = this.components.length - 1;
  }
  shimClick(_e: CustomEvent) {
    this.pickSelectedComponent();
  }

  pickSelectedComponent() {
    const component = this.components[this.selected];
    if (component && this.options) {
      // Make sure picker closes even if callback fails
      try {
        this.options.pickCallback(component);
      } catch (error) {
        console.error('Pick callback failed', error);
      }
    }
    this.close();
  }

  highlight(element: HTMLElement | undefined) {
    if (this.highlighted) {
      this.highlighted.classList.remove('vaadin-dev-tools-highlight');
    }
    this.highlighted = element;
    if (this.highlighted) {
      this.highlighted.classList.add('vaadin-dev-tools-highlight');
    }
  }
}
