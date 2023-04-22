import { css, html, LitElement, PropertyValues, render } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';
import { RgbaStringBase } from 'vanilla-colorful/lib/entrypoints/rgba-string';
// @ts-ignore
import { PositionMixin } from '../../../vendor/@vaadin/overlay/src/vaadin-overlay-position-mixin.js';

export class ColorPickerChangeEvent extends CustomEvent<{ value: string }> {
  constructor(value: string) {
    super('color-picker-change', { detail: { value } });
  }
}

const previewStyles = css`
  :host {
    --preview-size: 24px;
    --preview-color: rgba(0, 0, 0, 0);
  }

  .preview {
    --preview-bg-size: calc(var(--preview-size) / 2);
    --preview-bg-pos: calc(var(--preview-size) / 4);

    width: var(--preview-size);
    height: var(--preview-size);
    padding: 0;
    position: relative;
    overflow: hidden;
    background: none;
    border: solid 2px #888;
    border-radius: 4px;
    box-sizing: content-box;
  }

  .preview::before,
  .preview::after {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }

  .preview::before {
    content: '';
    background: white;
    background-image: linear-gradient(45deg, #666 25%, transparent 25%),
      linear-gradient(45deg, transparent 75%, #666 75%), linear-gradient(45deg, transparent 75%, #666 75%),
      linear-gradient(45deg, #666 25%, transparent 25%);
    background-size: var(--preview-bg-size) var(--preview-bg-size);
    background-position: 0 0, 0 0, calc(var(--preview-bg-pos) * -1) calc(var(--preview-bg-pos) * -1),
      var(--preview-bg-pos) var(--preview-bg-pos);
  }

  .preview::after {
    content: '';
    background-color: var(--preview-color);
  }
`;

@customElement('vaadin-dev-tools-color-picker')
export class ColorPicker extends LitElement {
  static get styles() {
    return [
      previewStyles,
      css`
        #toggle {
          display: block;
        }
      `
    ];
  }

  @property({})
  public value!: string;
  @property({})
  public presets?: string[];

  @query('#toggle')
  private toggle!: HTMLElement;
  private overlay!: VaadinOverlay;
  private commitValue: boolean = false;

  protected update(changedProperties: PropertyValues) {
    super.update(changedProperties);

    if (changedProperties.has('value') && this.overlay) {
      this.overlay.requestContentUpdate();
    }
  }

  protected firstUpdated() {
    // Create custom Vaadin overlay and wire up renderer and event listeners
    // Not creating this in the render function as Lit gets confused after
    // the overlay teleports itself to the document body
    this.overlay = document.createElement('vaadin-dev-tools-color-picker-overlay') as VaadinOverlay;
    this.overlay.renderer = this.renderOverlayContent.bind(this);
    this.overlay.owner = this;
    this.overlay.positionTarget = this.toggle;
    this.overlay.noVerticalOverlap = true;
    this.overlay.addEventListener('vaadin-overlay-escape-press', this.handleOverlayEscape.bind(this));
    this.overlay.addEventListener('vaadin-overlay-close', this.handleOverlayClose.bind(this));
    this.append(this.overlay);
  }

  render() {
    const previewColor = this.value || 'rgba(0, 0, 0, 0)';

    return html` <button
      id="toggle"
      class="preview"
      style="--preview-color: ${previewColor}"
      @click=${this.open}
    ></button>`;
  }

  private open() {
    this.commitValue = false;
    this.overlay.opened = true;
    // Seems zIndex is cleared on opening overlay, so set it after opening
    this.overlay.style.zIndex = '1000000';
    // Need to find better way of styling overlay parts...
    const overlayPart = this.overlay.shadowRoot!.querySelector('[part="overlay"]') as HTMLElement;
    overlayPart.style.background = '#333';
  }

  private renderOverlayContent(root: HTMLElement) {
    // vanilla-colorful requires an RGBA value, which we can get from computed
    // styles of the preview element
    const previewStyles = getComputedStyle(this.toggle, '::after');
    const color = previewStyles.getPropertyValue('background-color');

    render(
      html` <div>
        <vaadin-dev-tools-color-picker-overlay-content
          .value=${color}
          .presets=${this.presets}
          @color-changed=${this.handleColorChange.bind(this)}
        ></vaadin-dev-tools-color-picker-overlay-content>
      </div>`,
      root
    );
  }

  private handleColorChange(event: CustomEvent<{ value: string; close?: boolean }>) {
    this.commitValue = true;
    this.dispatchEvent(new ColorPickerChangeEvent(event.detail.value));
    if (event.detail.close) {
      this.overlay.opened = false;
      this.handleOverlayClose();
    }
  }

  private handleOverlayEscape() {
    this.commitValue = false;
  }

  private handleOverlayClose() {
    const eventType = this.commitValue ? 'color-picker-commit' : 'color-picker-cancel';
    this.dispatchEvent(new CustomEvent(eventType));
  }
}

@customElement('vaadin-dev-tools-color-picker-overlay-content')
export class ColorPickerOverlayContent extends LitElement {
  @property({})
  public value!: string;
  @property({})
  public presets?: string[];

  static get styles() {
    return [
      previewStyles,
      css`
        :host {
          display: block;
          padding: 12px;
        }

        .picker::part(saturation),
        .picker::part(hue) {
          margin-bottom: 10px;
        }

        .picker::part(hue),
        .picker::part(alpha) {
          flex: 0 0 20px;
        }

        .picker::part(saturation),
        .picker::part(hue),
        .picker::part(alpha) {
          border-radius: 3px;
        }

        .picker::part(saturation-pointer),
        .picker::part(hue-pointer),
        .picker::part(alpha-pointer) {
          width: 20px;
          height: 20px;
        }

        .swatches {
          display: grid;
          grid-template-columns: repeat(6, var(--preview-size));
          grid-column-gap: 10px;
          grid-row-gap: 6px;
          margin-top: 16px;
        }
      `
    ];
  }

  render() {
    return html` <div>
      <vaadin-dev-tools-rgba-string-color-picker
        class="picker"
        .color=${this.value}
        @color-changed=${this.handlePickerChange}
      ></vaadin-dev-tools-rgba-string-color-picker>
      ${this.renderSwatches()}
    </div>`;
  }

  renderSwatches() {
    if (!this.presets || this.presets.length === 0) {
      return;
    }

    const swatches = this.presets.map((preset) => {
      return html` <button
        class="preview"
        style="--preview-color: ${preset}"
        @click=${() => this.selectPreset(preset)}
      ></button>`;
    });

    return html` <div class="swatches">${swatches}</div>`;
  }

  private handlePickerChange(e: CustomEvent<{ value: string }>) {
    this.dispatchEvent(new CustomEvent('color-changed', { detail: { value: e.detail.value } }));
  }

  private selectPreset(preset: string) {
    this.dispatchEvent(new CustomEvent('color-changed', { detail: { value: preset, close: true } }));
  }
}

// Define basic interface for Vaadin Overlay
// Importing the interface is not possible as it breaks the Flow build
interface VaadinOverlay extends HTMLElement {
  opened: boolean;
  owner: HTMLElement;
  positionTarget: HTMLElement;
  noVerticalOverlap: boolean;

  renderer(root: HTMLElement): void;

  requestContentUpdate(): void;
}

// Define a custom overlay Polymer element that includes the position mixin.
// Importing the Overlay class is not possible as it breaks the Flow build
customElements.whenDefined('vaadin-overlay').then(() => {
  const OverlayClass = customElements.get('vaadin-overlay');

  class ColorPickerOverlay extends PositionMixin<any>(OverlayClass) {}

  customElements.define('vaadin-dev-tools-color-picker-overlay', ColorPickerOverlay as any);
});

// Define dev-tools specific element for the vanilla-colorful RGBA picker
customElements.define('vaadin-dev-tools-rgba-string-color-picker', RgbaStringBase);
