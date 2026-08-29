import { html, LitElement } from 'lit';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin';

/**
 * Test-only layout element used to verify Flow's per-component theme CSS.
 *
 * It renders a single `<slot>` so that the light-DOM children (three divs) are
 * slotted, and mixes in ThemableMixin so that styles registered via
 * `registerStyles('faux-horizontal-layout', ...)` — how Flow injects
 * `theme/components/faux-horizontal-layout.css` — reach the shadow DOM. The
 * per-component CSS colors the children with `::slotted(:nth-child(N))`, which
 * only works when the children are distributed through a real `<slot>`.
 *
 * The tag deliberately does not match any real Vaadin component, so that no
 * part of the test can be mistaken for exercising vaadin-horizontal-layout.
 */
class FauxHorizontalLayout extends ThemableMixin(LitElement) {
  static get is() {
    return 'faux-horizontal-layout';
  }

  render() {
    return html`<slot></slot>`;
  }
}

customElements.define(FauxHorizontalLayout.is, FauxHorizontalLayout);
