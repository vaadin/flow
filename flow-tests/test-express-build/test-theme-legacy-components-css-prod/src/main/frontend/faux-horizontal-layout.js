import { html, LitElement } from 'lit';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin';

/**
 * Test-only stand-in for the vaadin-horizontal-layout web component.
 *
 * It renders a single `<slot>` so that the light-DOM children (three divs) are
 * slotted, and mixes in ThemableMixin so that styles registered via
 * `registerStyles('vaadin-horizontal-layout', ...)` — how Flow injects
 * `theme/components/vaadin-horizontal-layout.css` — reach the shadow DOM. The
 * per-component CSS colors the children with `::slotted(:nth-child(N))`, which
 * only works when the children are distributed through a real `<slot>`.
 *
 * It is registered under the real `vaadin-horizontal-layout` tag so the
 * existing `components/vaadin-horizontal-layout.css` files keep matching.
 */
class FauxHorizontalLayout extends ThemableMixin(LitElement) {
  static get is() {
    return 'vaadin-horizontal-layout';
  }

  render() {
    return html`<slot></slot>`;
  }
}

customElements.define(FauxHorizontalLayout.is, FauxHorizontalLayout);
