import { html, LitElement } from 'lit';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin';

/**
 * Test-only input element used to verify Flow's per-component theme CSS.
 *
 * It reproduces just enough of a field's shadow-DOM shape (a `.field-container`
 * wrapper containing a `[part=input-field]` element) for the theme ITs to walk,
 * and mixes in ThemableMixin so that styles registered via
 * `registerStyles('faux-text-field', ...)` — how Flow injects
 * `theme/components/faux-text-field.css` — reach the shadow DOM. A plain custom
 * element would never receive those registered styles.
 *
 * Neither the tag nor anything in the shadow DOM matches a real Vaadin
 * component, so no part of the test can be mistaken for exercising
 * vaadin-text-field.
 */
class FauxTextField extends ThemableMixin(LitElement) {
  static get is() {
    return 'faux-text-field';
  }

  render() {
    return html`
      <div class="field-container">
        <div part="input-field">
          <slot></slot>
          <input />
        </div>
      </div>
    `;
  }
}

customElements.define(FauxTextField.is, FauxTextField);
