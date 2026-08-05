import { html, LitElement } from 'lit';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin';

/**
 * Test-only stand-in for the vaadin-text-field web component.
 *
 * It reproduces just enough of the shadow-DOM shape (a
 * `.vaadin-field-container` wrapper containing a `[part=input-field]` element)
 * for the theme ITs to walk, and mixes in ThemableMixin so that styles
 * registered via `registerStyles('vaadin-text-field', ...)` — which is how Flow
 * injects `theme/components/vaadin-text-field.css` — reach the shadow DOM. A
 * plain custom element would never receive those registered styles.
 *
 * It is deliberately registered under the real `vaadin-text-field` tag so the
 * existing `theme/components/vaadin-text-field.css` files and
 * `@CssImport(themeFor = "vaadin-text-field")` annotations keep matching without
 * any change.
 */
class ThemableInput extends ThemableMixin(LitElement) {
  static get is() {
    return 'vaadin-text-field';
  }

  render() {
    return html`
      <div class="vaadin-field-container">
        <vaadin-input-container part="input-field">
          <slot></slot>
          <input />
        </vaadin-input-container>
      </div>
    `;
  }
}

customElements.define(ThemableInput.is, ThemableInput);
