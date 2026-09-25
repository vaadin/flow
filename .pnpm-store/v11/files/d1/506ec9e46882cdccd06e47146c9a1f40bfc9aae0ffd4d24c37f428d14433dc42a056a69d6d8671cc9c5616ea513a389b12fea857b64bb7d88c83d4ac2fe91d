/**
 * @license
 * Copyright (c) 2017 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
export const ThemePropertyMixin = (superClass) =>
  class VaadinThemePropertyMixin extends superClass {
    static get properties() {
      return {
        /**
         * Helper property with theme attribute value facilitating propagation
         * in shadow DOM.
         *
         * Enables the component implementation to propagate the `theme`
         * attribute value to the sub-components in Shadow DOM by binding
         * the sub-component's "theme" attribute using the Lit template:
         *
         * ```html
         * <vaadin-notification-card
         *   theme="${ifDefined(this._theme)}"
         * ></vaadin-notification-card>
         * ```
         *
         * @protected
         */
        _theme: {
          type: String,
          readOnly: true,
        },
      };
    }

    static get observedAttributes() {
      return [...super.observedAttributes, 'theme'];
    }

    /** @protected */
    attributeChangedCallback(name, oldValue, newValue) {
      super.attributeChangedCallback(name, oldValue, newValue);

      if (name === 'theme') {
        this._set_theme(newValue);
      }
    }
  };
