import { css, html, LitElement } from 'lit';
import { customElement } from 'lit/decorators.js';

/**
 * Blocks interaction with all elements on the page. Forwards mouse and key events as custom shim-* events.
 */
@customElement('vaadin-dev-tools-shim')
export class Shim extends LitElement {
  highlighted?: HTMLElement;
  static shadowRootOptions = { ...LitElement.shadowRootOptions, delegatesFocus: true };

  static styles = [
    css`
      div {
        pointer-events: auto;
        background: rgba(255, 255, 255, 0);
        position: fixed;
        inset: 0px;
        z-index: 1000000;
      }
    `
  ];
  render() {
    return html`<div
      tabindex="-1"
      @mousemove=${this.onMouseMove}
      @click=${this.onClick}
      @keydown=${this.onKeyDown}
    ></div>`;
  }

  onClick(e: MouseEvent) {
    const target = this.getTargetElement(e);
    this.dispatchEvent(new CustomEvent('shim-click', { detail: { target } }));
  }
  onMouseMove(e: MouseEvent) {
    const target = this.getTargetElement(e);
    this.dispatchEvent(new CustomEvent('shim-mousemove', { detail: { target } }));
  }
  onKeyDown(e: KeyboardEvent) {
    this.dispatchEvent(new CustomEvent('shim-keydown', { detail: { originalEvent: e } }));
  }

  getTargetElement(e: MouseEvent): HTMLElement {
    this.style.display = 'none';
    const targetElement = document.elementFromPoint(e.clientX, e.clientY) as HTMLElement;
    this.style.display = '';

    return targetElement;
  }
}
