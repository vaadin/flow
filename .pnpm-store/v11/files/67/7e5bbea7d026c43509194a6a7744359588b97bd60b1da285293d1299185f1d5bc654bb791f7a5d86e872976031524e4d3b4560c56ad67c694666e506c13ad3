/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * @polymerMixin
 */
export const InputContainerMixin = (superClass) =>
  class InputContainerMixinClass extends superClass {
    static get properties() {
      return {
        /**
         * If true, the user cannot interact with this element.
         */
        disabled: {
          type: Boolean,
          reflectToAttribute: true,
        },

        /**
         * Set to true to make this element read-only.
         */
        readonly: {
          type: Boolean,
          reflectToAttribute: true,
        },

        /**
         * Set to true when the element is invalid.
         */
        invalid: {
          type: Boolean,
          reflectToAttribute: true,
        },
      };
    }

    /** @protected */
    ready() {
      super.ready();

      this.addEventListener('pointerdown', (event) => {
        if (event.target === this) {
          // Prevent direct clicks to the input container from blurring the input
          event.preventDefault();
        }
      });

      this.addEventListener('click', (event) => {
        if (event.target === this) {
          // The vaadin-input-container element was directly clicked,
          // focus any focusable child element from the default slot
          this.shadowRoot
            .querySelector('slot:not([name])')
            .assignedNodes({ flatten: true })
            .forEach((node) => node.focus && node.focus());
        }
      });
    }
  };
